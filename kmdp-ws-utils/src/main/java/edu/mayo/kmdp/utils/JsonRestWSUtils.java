/**
 * Copyright © 2018 Mayo Clinic (RSTKNOWLEDGEMGMT@mayo.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.mayo.kmdp.utils;

import ca.uhn.fhir.model.api.BaseElement;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.threetenbp.ThreeTenModule;
import edu.mayo.kmdp.util.JSonUtil;
import javassist.*;
import org.hl7.fhir.dstu3.model.Base;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonRestWSUtils {

  static {
    ClassPool.getDefault().insertClassPath(new ClassClassPath(JsonRestWSUtils.class));
  }

  public static enum WithFHIR {
    DSTU2, STU3, NONE;
  }

  public static MappingJackson2HttpMessageConverter jacksonFHIRAdapter() {
    return jacksonFHIRAdapter(WithFHIR.NONE);
  }

  public static MappingJackson2HttpMessageConverter jacksonFHIRAdapter(WithFHIR fhir) {
    return buildFHIRAdapter(fhir);
  }

  public static <T> T configuredClient(Class<T> webClientClass) {
    return configuredClient(webClientClass, WithFHIR.NONE);
  }

  public static <T> T configuredClient(Class<T> webClientClass, JsonRestWSUtils.WithFHIR fhir) {
    return fhirEnabledClient(webClientClass, fhir).get();
  }

  public static RestTemplate restTemplate() {
    return buildRestTemplate(WithFHIR.NONE);
  }

  public static RestTemplate restTemplate(WithFHIR fhir) {
    return buildRestTemplate(fhir);
  }

  protected static ObjectMapper getObjectMapper(WithFHIR fhir) {
    return configure(new ObjectMapper(), fhir);
  }

  protected static ObjectMapper configure(ObjectMapper objectMapper, WithFHIR fhir) {
    objectMapper = JSonUtil.configureMapper(objectMapper);

    objectMapper.registerModule(new ThreeTenModule());

    try {
      switch (fhir) {
        case DSTU2:
          objectMapper.registerModule(
              (Module) Class.forName("edu.mayo.kmdp.util.fhir2.json.FHIR2JacksonModule")
                  .newInstance());
          break;
        case STU3:
          objectMapper.registerModule(
              (Module) Class.forName("edu.mayo.kmdp.util.fhir3.json.FHIR3JacksonModule")
                  .newInstance());
          break;
        case NONE:
        default:
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return objectMapper;
  }

  private static RestTemplate configure(RestTemplate restTemplate, WithFHIR fhir) {
    MappingJackson2HttpMessageConverter messageConverter = restTemplate.getMessageConverters()
        .stream()
        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
        .map(MappingJackson2HttpMessageConverter.class::cast)
        .findFirst().orElseThrow(
            () -> new IllegalStateException("MappingJackson2HttpMessageConverter not found"));

    configure(messageConverter.getObjectMapper(), fhir);

    return restTemplate;
  }

  protected static RestTemplate buildRestTemplate(WithFHIR fhir) {
    RestTemplate restTemplate = new RestTemplate();

    Set<AbstractJackson2HttpMessageConverter> defaultJacksonConverters =
        restTemplate.getMessageConverters().stream()
            .filter(AbstractJackson2HttpMessageConverter.class::isInstance)
            .map(AbstractJackson2HttpMessageConverter.class::cast)
            .collect(Collectors.toSet());
    restTemplate.getMessageConverters().removeAll(defaultJacksonConverters);
    restTemplate.getMessageConverters().add(buildFHIRAdapter(fhir));

    // This allows us to read the response more than once - Necessary for debugging.
    restTemplate
        .setRequestFactory(new BufferingClientHttpRequestFactory(restTemplate.getRequestFactory()));

    return configure(restTemplate, fhir);
  }


  protected static <T> Optional<T> fhirEnabledClient(Class<T> apiClient,
      JsonRestWSUtils.WithFHIR version) {
    try {
      if (version == JsonRestWSUtils.WithFHIR.NONE) {
        return Optional.ofNullable(apiClient.newInstance());
      }
      String fhirProxy = apiClient.getName() + "_" + version.name();
      Class<?> fhirProxyClass;

      try {
        fhirProxyClass = Class.forName(fhirProxy);
      } catch (ClassNotFoundException cnfe) {
        CtClass clientClass = ClassPool.getDefault().get(apiClient.getName());
        CtClass fhirClient = ClassPool.getDefault()
            .makeClass(apiClient.getName() + "_" + version.name(), clientClass);

        String self = JsonRestWSUtils.class.getName();
        String src =
            "protected org.springframework.web.client.RestTemplate buildRestTemplate() { " +
                "return " + self + ".restTemplate( " + self + ".WithFHIR." + version.name() + " ); "
                +
                "}";
        CtMethod m = CtNewMethod.make(
            src,
            fhirClient);
        fhirClient.addMethod(m);
        fhirProxyClass = fhirClient.toClass();
      }
      return fhirProxyClass != null ? Optional.of((T) fhirProxyClass.newInstance())
          : Optional.empty();

    } catch (NotFoundException | CannotCompileException | IllegalAccessException | InstantiationException e) {
      e.printStackTrace();
    }
    return Optional.empty();
  }


  private static MappingJackson2HttpMessageConverter buildFHIRAdapter(WithFHIR fhir) {
    MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter() {
      @Override
      protected JavaType getJavaType(Type type, Class<?> contextClass) {
        if (type instanceof Class<?> && java.util.Map.class.isAssignableFrom((Class<?>) type)) {
          switch (fhir) {
            case STU3:
              return TypeFactory.defaultInstance()
                  .constructMapType(Map.class, String.class, Base.class);
            case DSTU2:
              return TypeFactory.defaultInstance()
                  .constructMapType(Map.class, String.class, BaseElement.class);
            case NONE:
            default:
              return super.getJavaType(type, contextClass);
          }
        } else {
          return super.getJavaType(type, contextClass);
        }
      }
    };
    jsonConverter.setObjectMapper(getObjectMapper(fhir));
    return jsonConverter;
  }


}