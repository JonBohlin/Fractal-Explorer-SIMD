import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Explorer {
    public Explorer( int s_x, int s_y){
        JFrame MandelbrotFrame = new JFrame("Mandelbrot");
        var MandelbrotJPanel = new MandelbrotFractalJPanel( s_x/2, s_y/2);
        MandelbrotFrame.add( MandelbrotJPanel );
        MandelbrotFrame.setSize( s_x/2, s_y/2 );
        MandelbrotFrame.setLocation(s_x + 10, s_y/4 + 10);
        MandelbrotFrame.setVisible( true );
        MandelbrotFrame.setResizable( false );
    }
}
