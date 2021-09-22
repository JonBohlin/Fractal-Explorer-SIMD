import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import jdk.incubator.vector.*;
import static jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED;

public class MandelbrotFractalJPanel extends JuliaFractalJPanel{
    protected FractalCoordinates fc = new FractalCoordinates();
    private int screenWidth;
    private int screenHeight;

    public MandelbrotFractalJPanel(int s_x, int s_y) {
        super(s_x, s_y);
        screenWidth = s_x;
        screenHeight = s_y;
        offset.x = -2.0;
        offset.y = -1.2;
        startPan.x = 0.0;
        startPan.y = 0.0;

        frac_tl.x = -2.0;
        frac_tl.y = -1.0;
        frac_br.x = 1.0;
        frac_br.y = 1.0;
        vScale = s_x;
        JFrame JuliaSetFrame = new JFrame("Julia set");
        var JuliaJPanel = new JuliaFractalJPanel(2*screenWidth, 2*screenHeight);
        JuliaSetFrame.add(JuliaJPanel);
        JuliaSetFrame.setSize( 2*screenWidth, 2*screenHeight);
        JuliaSetFrame.setLocation(0,0);
        JuliaSetFrame.setVisible(true);
        JuliaSetFrame.setResizable(false);

        JFrame SmallJuliaSetFrame = new JFrame("Mandelbrot - Julia set");
        var SmallJuliaJPanel = new JuliaFractalJPanel(screenWidth / 2, screenHeight / 2);
        SmallJuliaSetFrame.add(SmallJuliaJPanel);
        SmallJuliaSetFrame.setSize(screenWidth/2, screenHeight / 2);
        SmallJuliaSetFrame.setLocation(2*screenWidth + 10,0);
        SmallJuliaSetFrame.setVisible(true);
        SmallJuliaSetFrame.setResizable(false);

        JuliaSetFrame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                String filename;
                Date date = new Date();
                DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_dd_ss");
                filename=dateFormat.format(date)+".png";
                File outputFile = new File(filename);

                if(keyEvent.getKeyChar()=='s') {
                    System.out.println("Saving fractal to file:" + filename);
                    try {
                        ImageIO.write(JuliaJPanel.screenshot, "png", outputFile);
                    } catch (IOException evt) {
                        System.out.println("" + evt);
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {

            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {

            }
        });

        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        mouseCoordinates.x = e.getX();
                        mouseCoordinates.y = e.getY();
                        screenToFractal(mouseCoordinates, fc);
                        JuliaJPanel.cr = fc.x;
                        JuliaJPanel.ci = fc.y;
                        JuliaJPanel.resetCoordinates();
                        JuliaJPanel.repaint();
                    }
                }
        );

        addMouseMotionListener(
                new MouseMotionAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        mouseCoordinates.x = e.getX();
                        mouseCoordinates.y = e.getY();
                        screenToFractal(mouseCoordinates, fc);
                        SmallJuliaJPanel.cr = fc.x;
                        SmallJuliaJPanel.ci = fc.y;
                        SmallJuliaJPanel.repaint();
                        System.out.println(fc.x + "+i" + fc.y);
                    }
                }
        );
    }

    @Override
    protected void CreateFractal(final PixelCoordinates lPix_tl,final PixelCoordinates lPix_br,final FractalCoordinates lFrac_tl, final FractalCoordinates lFrac_br, final int lIterations ){
        final double x_scale = (lFrac_br.x - lFrac_tl.x) / ((double) lPix_br.x - (double) lPix_tl.x);
        final double y_scale = (lFrac_br.y - lFrac_tl.y) / ((double) lPix_br.y - (double) lPix_tl.y);
        // Make it SIMD agnostic
        final VectorSpecies<Double> SPECIES = SPECIES_PREFERRED;
        double index[];
        int intArray[];

        index = new double[SPECIES.length()];
        for(int i = 0 ; i<SPECIES.length(); i++)
            index[i] = i;

        double y_pos = lFrac_tl.y;
        int y_offset = lPix_tl.y*screenWidth;

        DoubleVector _a, _b, _ci, _x_pos, _cr, _zr, _zi, _zr2, _zi2;
        LongVector _n, _c, _c_temp;
        VectorMask<Double> _mask1;
        VectorMask<Long> _mask2;
//		IntVector __n;

        LongVector _iterations = LongVector.broadcast(LongVector.SPECIES_PREFERRED, lIterations );
        LongVector _one = LongVector.broadcast(LongVector.SPECIES_PREFERRED, 1);

        DoubleVector _x_scale = DoubleVector.broadcast(SPECIES, x_scale);
        DoubleVector _x_jump = DoubleVector.broadcast(SPECIES, x_scale * SPECIES.length() );
        DoubleVector _two = DoubleVector.broadcast(SPECIES, 2.0d);
        DoubleVector _four = DoubleVector.broadcast(SPECIES, 4.0d);
        DoubleVector _x_pos_offsets = DoubleVector.fromArray(SPECIES, index, 0 );

        _x_pos_offsets = _x_pos_offsets.mul( _x_scale );

        for (int y = lPix_tl.y; y < lPix_br.y; y++){
            _a = DoubleVector.broadcast(SPECIES, lFrac_tl.x);
            _x_pos = _a.add(_x_pos_offsets);
            _ci = DoubleVector.broadcast( SPECIES, y_pos);

            for (int x = lPix_tl.x; x < lPix_br.x; x+=SPECIES.length()){
                _cr = _x_pos;
                _zr = DoubleVector.zero(SPECIES_PREFERRED);
                _zi = DoubleVector.zero(SPECIES_PREFERRED);
                _n = LongVector.zero(LongVector.SPECIES_PREFERRED);

                do{
                    _zr2 = _zr.mul( _zr );
                    _zi2 = _zi.mul( _zi );

                    _a = _zr2.sub( _zi2 );
                    _a = _a.add( _cr );
                    _b = _zr.mul( _zi );
                    _b = _b.mul( _two );
                    _b = _b.add( _ci );

                    _zr = _a;
                    _zi = _b;
                    _a = _zr2.add( _zi2 );

                    // z<2 & iter < maxIter
                    _mask1 = _a.compare(VectorOperators.LE, _four);
                    _mask2 = _n.compare(VectorOperators.LE, _iterations);
                    _c_temp = _mask2.and(_mask1.cast(LongVector.SPECIES_PREFERRED)).toVector().reinterpretAsLongs();

                    _c = _one.and( _c_temp );
                    _n = _n.add(_c);
                    //__n = _n.reinterpretAsInts();

                } while(_mask2.and(_mask1.cast(LongVector.SPECIES_PREFERRED)).anyTrue() );

                _x_pos =_x_pos.add(_x_jump);
                if( y_offset + x + SPECIES.length() < fractalColors.length ){
                    //__n.intoArray( fractalColors, y_offset + x);
                    intArray = _n.toIntArray();
                    for(int i = 0; i < intArray.length; i++)
                        fractalColors[y_offset + x + i] = intArray[ i ];
                }
            }
            y_pos += y_scale;
            y_offset += screenWidth;
        }
    }
}
