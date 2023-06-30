package edu.mayo.kmdp;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGRect;
import org.w3c.dom.svg.SVGSVGElement;

public class SVGRenderer extends JFrame {

  private final JSVGCanvas canvas;


  public SVGRenderer() {
    setTitle("SVG Viewer");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Create the SVG canvas
    canvas = new JSVGCanvas();
    canvas.setEnableImageZoomInteractor(true);
    canvas.setEnablePanInteractor(true);

    // Create a panel and add the canvas to it
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(canvas, BorderLayout.CENTER);
    getContentPane().add(panel);

    pack();
    setSize(1024,768);
    setLocationRelativeTo(null);
    setVisible(true);
  }

  public void loadSVG(URL svgFile) {
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    SVGDocument svgDocument = (SVGDocument) domImpl.createDocument(
        SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null);
    svgDocument.getRootElement().setAttribute("preserveAspectRatio", "true");
    canvas.setSVGDocument(svgDocument);
    canvas.loadSVGDocument(svgFile.toString());
  }

}
