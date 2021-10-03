import jdk.incubator.vector.*;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.Math;

import static jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED;

class FractalCoordinates{
	double x,y;
}

class PixelCoordinates{
	int x,y;
}

public class JuliaFractalJPanel extends JPanel{//} implements MouseWheelListener{
	protected int screenWidth;
	protected int screenHeight;
	protected FractalCoordinates offset = new FractalCoordinates();
	protected FractalCoordinates startPan = new FractalCoordinates();
	protected FractalCoordinates mouseCoordinatesBeforeZoom = new FractalCoordinates();
	protected FractalCoordinates mouseCoordinatesAfterZoom = new FractalCoordinates();
	protected FractalCoordinates frac_tl = new FractalCoordinates();
	protected FractalCoordinates frac_br = new FractalCoordinates();
	protected PixelCoordinates mouseCoordinates = new PixelCoordinates();
	protected PixelCoordinates pix_tl = new PixelCoordinates();
	protected PixelCoordinates pix_br = new PixelCoordinates();
	protected BufferedImage screenshot;

	protected double vScale;
 	protected int maxThreads = Runtime.getRuntime().availableProcessors();
	protected final int defaultInter = 128;
	protected int nIterations = defaultInter;
	protected int[] fractalColors;
	protected final int numThreads = 10;
	protected double cr = -0.4;
 	protected double ci = 0.6;
		
	public JuliaFractalJPanel(int s_x, int s_y){
		resetCoordinates();
		pix_tl.x=0; pix_tl.y=0;
		screenWidth = s_x;
		screenHeight = s_y;
		vScale = s_x/2;
		pix_br.x=screenWidth; pix_br.y=screenHeight;
		System.out.println("Number of physical cores:"+maxThreads);
		fractalColors= new int[ screenWidth * screenHeight ];

		addMouseMotionListener(
        	new MouseMotionAdapter(){
        		public void mouseMoved( MouseEvent e ){
        			mouseCoordinates.x = e.getX();
					mouseCoordinates.y = e.getY();
       			}
					
				public void mouseDragged( MouseEvent e ){
						startPan.x = e.getX();
						startPan.y = e.getY();
						offset.x -= (mouseCoordinates.x - startPan.x )/vScale;
						offset.y -= (mouseCoordinates.y - startPan.y )/vScale;
						startPan.x = mouseCoordinates.x;
						startPan.y = mouseCoordinates.y;
					}
				}
			);

		addMouseWheelListener(
				e -> {

					screenToFractal(mouseCoordinates ,mouseCoordinatesBeforeZoom);
					startPan.x = mouseCoordinates.x;
					startPan.y = mouseCoordinates.y;

       				if( e.isControlDown() && e.getWheelRotation() < 0 ){
       					nIterations+=64;
       					System.out.println("Iterations :"+nIterations);
       				}

       				if( e.isControlDown() && e.getWheelRotation() > 0 && nIterations>64 ){
       					nIterations-=64;
	       				System.out.println("Iterations :"+nIterations);
	       			}

					if (e.getWheelRotation() < 0 && !e.isControlDown() && !e.isShiftDown() && !e.isAltDown())
        				vScale*=1.1;

					if (e.getWheelRotation() > 0 && !e.isControlDown() && !e.isShiftDown() && !e.isAltDown())
        				vScale*=0.9;

        			screenToFractal(mouseCoordinates ,mouseCoordinatesAfterZoom);
        			offset.x += (mouseCoordinatesBeforeZoom.x - mouseCoordinatesAfterZoom.x);
        			offset.y += (mouseCoordinatesBeforeZoom.y - mouseCoordinatesAfterZoom.y);
        		}
		);
	}

	protected FractalCoordinates screenToFractal(PixelCoordinates p ,FractalCoordinates f){
		f.x = ((double) p.x /vScale*2.5)+offset.x;
		f.y = ((double) p.y /vScale*2.5)+offset.y;
		return f;
	}

	public void resetCoordinates(){
		offset.x= -2.5;
		offset.y= -2.5;
		startPan.x = 0.0;
		startPan.y = 0.0;

		frac_tl.x = -2.0; frac_tl.y = -1.0;
		frac_br.x = 1.0; frac_br.y = 1.0;
		vScale = screenWidth/2;
		nIterations = defaultInter;
	}

	@Override
	public void update( Graphics g ){
		Color myColor;
		BufferedImage br= new BufferedImage(screenWidth,screenHeight,BufferedImage.TYPE_INT_RGB);
		frac_tl = screenToFractal( pix_tl, frac_tl);
		frac_br = screenToFractal( pix_br, frac_br);
		MultiThreadFractal(pix_tl, pix_br, frac_tl, frac_br, cr, ci, nIterations);
		for(int y=0; y<screenHeight; y++){
			for(int x=0; x<screenWidth; x++){
				int col = fractalColors[y*screenWidth + x];
				final double a = 0.1;
				int red = (int)((1.0-(0.5*Math.sin(a* (double) col)+0.5))*256);
				int green = (int)((1.0-(0.5*Math.sin(a* (double) col + 2.094)+0.5))*256);
				int blue = (int)((1.0-(0.5*Math.sin(a* (double) col +4.188)+0.5))*256);
				myColor = new Color(red, green, blue);
				br.setRGB( x,y, myColor.getRGB() );
			}
		}
		g.drawImage(br, 0 ,0 ,null);
	}

	public void paintComponent( Graphics g ){
		BufferedImage bufferedImage = new BufferedImage(screenWidth,screenHeight,BufferedImage.TYPE_INT_RGB);
		Graphics gg = bufferedImage.getGraphics();

		update( gg );
		g.drawImage( bufferedImage, 0, 0, null);
		screenshot = bufferedImage;
	}

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

		_ci = DoubleVector.broadcast(SPECIES_PREFERRED, ci);
		_cr = DoubleVector.broadcast(SPECIES_PREFERRED, cr);

		for (int y = lPix_tl.y; y < lPix_br.y; y++){
			_a = DoubleVector.broadcast(SPECIES, lFrac_tl.x);
			_x_pos = _a.add(_x_pos_offsets);


			for (int x = lPix_tl.x; x < lPix_br.x; x+=SPECIES.length()){

				_zr = _x_pos;
				_zi = DoubleVector.broadcast(SPECIES_PREFERRED, y_pos );
				_n = LongVector.zero(LongVector.SPECIES_PREFERRED);

				do{
					_zr2 = _zr.mul( _zr );
					_zi2 = _zi.mul( _zi );
					_a = _zr2.sub( _zi2 );
					_a = _a.add( _cr );
					_b = _zr.mul( _zi );
// _b.fma(_two, _ci): _b=_b*_two+_ci
					_b=_b.fma(_two, _ci);
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

	protected void MultiThreadFractal(final PixelCoordinates lpix_tl,final PixelCoordinates lpix_br,final FractalCoordinates lfrac_tl, final FractalCoordinates lfrac_br, final double cr, final double ci, final int iterations ){
		final int sectionHeight = (lpix_br.y - lpix_tl.y)/numThreads;
		final double fractalHeight = (lfrac_br.y - lfrac_tl.y)/ (double) numThreads;
		FractalCoordinates tFrac_tl = new FractalCoordinates();
		FractalCoordinates tFrac_br = new FractalCoordinates();
		PixelCoordinates tPix_tl = new PixelCoordinates();
		PixelCoordinates tPix_br = new PixelCoordinates();

		final Thread[] myThreads = new Thread[ numThreads + 1 ];
		tPix_tl.x = lpix_tl.x;
		tPix_br.x = lpix_br.x;			
		tFrac_tl.x = lfrac_tl.x;
		tFrac_br.x = lfrac_br.x;

		int i = 0;
		int j;
		for(; i < numThreads + 1; i++) {
			j = i - 1;

			tPix_tl.y = lpix_tl.y + sectionHeight * (j);
			tPix_br.y = lpix_tl.y + sectionHeight * (j + 1);
			tFrac_tl.y = lfrac_tl.y + fractalHeight * (double) j;
			tFrac_br.y = lfrac_tl.y + fractalHeight * (j + 1.0);
			myThreads[i] = new Thread(() -> CreateFractal(tPix_tl, tPix_br, tFrac_tl, tFrac_br, iterations));
			myThreads[i].start();
		}

		i = 0;
		for(; i < numThreads + 1; i++){
        	try{
        		myThreads[i].join();
        	} catch (InterruptedException e){
        		System.out.println("Thread interrupted");
       		}
       	}

       	repaint();
   	}
}