import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

enum Direction
{
	UP, DOWN, LEFT, RIGHT;
	
	Direction getOppositeDirection(Direction dir)
	{
		if (dir.equals(Direction.UP))
			return Direction.DOWN;
		else if (dir.equals(Direction.DOWN))
			return Direction.UP;
		else if (dir.equals(Direction.LEFT))
			return Direction.RIGHT;
		else
			return Direction.LEFT;
	}
	
	static Direction randomDirection()
	{
		return Direction.values()[(int)(Math.random() * 4)];
	}
}

public class CanvasRunner extends Application
{
	public static final double xDim, yDim;
	private static final Canvas canvas;
	public static final GraphicsContext gc;
	private static final Color backgroundColor;
	private static final String backgroundColorString;
	private static ShapeDrawerThread shapeDrawer;
	private static WritableImage image;
	private static boolean occupied;
	private static final Object lock;
	//private static final Object lock2;
	public static final int numTrianglePairs;
	public static final int delay;
	private static TimerThread timerThread;
	public static final int iterationCount;
	public static boolean waitForMouseClick;
	public static Triangle a;
	public static Triangle b;
	public static boolean currentA;
	public static final int triangleSize;
	public static final int circleSize;
	public static final int circleMinSize;
	public static final double circleSpawnRate;
	private static int occupiedCount;
	private static Pane root;
	
	static
	{
		xDim = 900;
		yDim = 900;
		numTrianglePairs = 175;
		delay = 225;
		iterationCount = 100;
		triangleSize = 400;
		circleSize = 100;
		circleMinSize = 20;
		circleSpawnRate = 0.75;
		occupiedCount = 0;
		
		//backgroundColorString = "#FFD700";
		//backgroundColor = Color.web(backgroundColorString);
		backgroundColorString = "#ffffff";
		backgroundColor = Color.web(backgroundColorString);
		
		canvas = new Canvas(xDim, yDim);
		gc = canvas.getGraphicsContext2D();
		
		occupied = false;
		waitForMouseClick = false;
		lock = new Object();
		//lock2 = new Object();
		
		a = null;
		b = null;
		
		currentA = true;
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}
	@Override
	public void start(Stage primaryStage)
	{
		primaryStage.setTitle("Triangle Design Generator");
	    root = new Pane();
	    root.setBackground(new Background(new BackgroundFill(Paint.valueOf("rgb(" + randRGB() + "," + randRGB() + "," + randRGB() + ")"/*"#000000"*/), CornerRadii.EMPTY, Insets.EMPTY)));
	    root.getChildren().add(canvas);
	    
	    shapeDrawer = new ShapeDrawerThread();
	    
	    /*root.setOnMouseClicked((new EventHandler<MouseEvent>()
		{
			/*@Override
			public void handle(KeyEvent event)
			{
				primaryStage.close();
				shapeDrawer.stop();
			}

			@Override
			public void handle(MouseEvent click)
			{
				synchronized (lock2)
				{
					if (waitForMouseClick)
					{
						System.out.println("Handling mouse click...");
						
						if (currentA)
						{
							a = new Triangle(b.v1x, b.v1y, b.v2x, b.v2y, (int) click.getX(), (int) click.getY());
						}
						else
						{
							b = new Triangle(a.v1x, a.v1y, a.v2x, a.v2y, (int) click.getX(), (int) click.getY());
						}
						
						lock.notify();
					}
				}
			}
		}));*/
	    
	    primaryStage.setScene(new Scene(root));
	    primaryStage.show();
	    
	    primaryStage.setOnCloseRequest(e ->
	    {
	    	System.out.println("Commencing shutdown...");
	    	shapeDrawer.stop();
			
			/*Image img = new Image("file:test.png");
			int width = (int) img.getWidth();
			int height = (int) img.getHeight();
			PixelReader reader = img.getPixelReader();
			byte[] buffer = new byte[width * height * 4];
			WritablePixelFormat<ByteBuffer> format = PixelFormat.getByteBgraInstance();
			reader.getPixels(0, 0, width, height, format, buffer, 0, width * 4);
			try {
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("test.data"));
				for(int count = 0; count < buffer.length; count += 4) {
					out.write(buffer[count + 2]);
					out.write(buffer[count + 1]);
					out.write(buffer[count]);
					out.write(buffer[count + 3]);
				}
				out.flush();
				out.close();
			} catch(IOException dsf)
			{
				dsf.printStackTrace();
			}*/
	    	
	    	save();
	    	
	    	primaryStage.close();
	    });
	    
	    shapeDrawer.start();
	}
	
	private static int randomX()
	{
		return (int)(Math.random() * CanvasRunner.xDim);
	}
	
	private static int randomY()
	{
		return (int)(Math.random() * CanvasRunner.yDim);
	}
	
	public static void setRandomBackgroundColor()
	{
		root.setBackground(new Background(new BackgroundFill(Paint.valueOf("rgb(" + randRGB() + "," + randRGB() + "," + randRGB() + ")"/*"#000000"*/), CornerRadii.EMPTY, Insets.EMPTY)));
	}
	
	private static int randRGB()
	{
		return (int)(Math.random() * 256);
	}
	
	private void save()
	{
		synchronized (lock)
		{
			BufferedImage bImage = SwingFXUtils.fromFXImage(root.snapshot(null, null), null);
	    	
	    	SimpleDateFormat s = new SimpleDateFormat("MM-dd-yyyy");
	    	
	    	File fileDirectory = new File(System.getProperty("user.dir") + File.separator + s.format(new Date()) + File.separator);
	    	if (!fileDirectory.exists())
	    		fileDirectory.mkdir();
	    	
	    	int numFiles = fileDirectory.listFiles().length;
	    	
	    	try
	    	{
				ImageIO.write(bImage, "png", new File(fileDirectory.toString() + File.separator + numFiles + ".png"));
				System.out.println("Success!");
			}
	    	catch (IOException e1)
	    	{
				e1.printStackTrace();
			}
	    	
	    	lock.notify();
		}
	}
	
	public static boolean isOccupied(int x, int y)
	{
		occupied = !image.getPixelReader().getColor(x, y).equals(backgroundColor);
		
			/*System.out.println("Pixel color - " + image.getPixelReader().getColor(x, y));
			System.out.println("Background color - " + backgroundColor.WHITE);
			System.out.println("Occupied - " + occupied + "\n");*/
		
		//System.out.println(occupied);
		
		//System.out.println(occupied);
		
		if (occupied)
		{
			occupiedCount++;
		}
		else
		{
			occupiedCount = 0;
		}
		
		//System.out.println(occupiedCount);
		
		return occupied;
	}
	
	public static void updateImage()
	{
		Platform.runLater(() ->
		{
			image = gc.getCanvas().snapshot(null, null);
		});
	}
	
	public static Color getBackgroundColor()
	{
		return backgroundColor;
	}
	
	private class ShapeDrawerThread extends Thread
	{
		private Thread drawerThread;
		private Runnable r;
		
		public ShapeDrawerThread()
		{
			super(new Runnable()
					{
						@Override
						public void run()
						{
							drawShapes(gc);
						}
					});
		}
		
		public void restart()
		{
			interrupt();
			r.run();
		}
		
		public void hold()
		{
			synchronized(lock)
			{
				try {
					lock.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private class TimerThread extends Thread
	{
		private Thread timerThread;
		private int delay;
		private Runnable r;
		
		public TimerThread(int delay)
		{
			super(new Runnable()
					{
						@Override
						public void run()
						{
							while (true)
							{
								try
								{
									Thread.sleep(delay);
								} catch (InterruptedException e)
								{
									e.printStackTrace();
								}
								
								System.out.println("Done sleeping");
								
								Platform.runLater(() -> save());
						    	
						    	//try
						    	//{
									shapeDrawer.hold();
								//}
						    	/*catch (InterruptedException e)
						    	{
									e.printStackTrace();
								}*/
						    	
						    	gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
						    	shapeDrawer.notify();
						    	shapeDrawer.restart();
							}
						}
					});
			
			this.r = r;
			this.delay = delay;
		}
		
		public void resetTimer()
		{
			interrupt();
			r.run();
		}
	}
	
    private void drawShapes(GraphicsContext gc)
    {
    	//timerThread = new TimerThread(4000);//3 * delay * numTrianglePairs);
    	//timerThread.start();
    	
    	while (true)
		{
    		synchronized(lock)
    		{
		    	gc.setLineWidth(1);
				gc.setStroke(Color.BLACK);
		    	
		    	try
		    	{
		    		Thread.sleep(250);
		    	}
		    	catch (InterruptedException e)
		    	{
		    		
		    	}
		    	
		    	updateImage();
		    	
		    	a = new Triangle(triangleSize, b);
				a.drawTriangle(gc);
		    	
				try
		    	{
		    		Thread.sleep(250);
		    	}
		    	catch (InterruptedException e)
		    	{
		    		
		    	}
				
		    	for (int i = 0; i < numTrianglePairs; i++)
		    	{
		    		currentA = false;
		    		
		    		b = new Triangle(triangleSize, a/*, a.getSavedDirection()*/, 2);
		    		
		    		if (waitForMouseClick)
		    		{
		    			Platform.runLater(() ->
		    			{
			    			shapeDrawer.stop();
			    			waitForMouseClick = false;
			    			System.out.println("About to save...");
			    			save();
			    			gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
			    			occupiedCount = 0;
			    			CanvasRunner.setRandomBackgroundColor();
			    			shapeDrawer = new ShapeDrawerThread();
			    			shapeDrawer.start();
			    			System.out.println("Starting thread again...");
		    			});
		    		}
		    		
		    		b.drawTriangle(gc);
		    		
		    		if (Math.random() < circleSpawnRate)
		    		{
		    			new Circle().drawCircle(gc);
		    		}
		    		
		    		try
		        	{
		        		Thread.sleep(delay);
		        	}
		        	catch (InterruptedException e)
		        	{
		        		
		        	}
		    		
		    		currentA = true;
		    		
		    		a = new Triangle(triangleSize, b/*, b.getSavedDirection()*/, 2);
		    		
		    		if (waitForMouseClick)
		    		{
		    			Platform.runLater(() ->
		    			{
			    			shapeDrawer.stop();
			    			waitForMouseClick = false;
			    			System.out.println("About to save...");
			    			save();
			    			gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
			    			occupiedCount = 0;
			    			CanvasRunner.setRandomBackgroundColor();
			    			shapeDrawer = new ShapeDrawerThread();
			    			shapeDrawer.start();
			    			System.out.println("Starting thread again...");
		    			});
		    		}
		    		
		    		a.drawTriangle(gc);
		    		
		    		if (Math.random() < circleSpawnRate)
		    		{
		    			new Circle(a.v3x, a.v3y).drawCircle(gc);
		    		}
		    		
		    		/*System.out.println("A - " + a.getSavedDirection());
		    		System.out.println("B - " + b.getSavedDirection());
		    		System.out.println();*/
		    		
		    		try
		        	{
		        		Thread.sleep(delay);
		        	}
		        	catch (InterruptedException e)
		        	{
		        		
		        	}
		    	}
		    	
		    	Platform.runLater(() -> save());
		    	
		    	try
		    	{
					lock.wait();
				}
		    	catch (InterruptedException e)
		    	{
					e.printStackTrace();
				}
		    	
		    	gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		    	//timerThread.resetTimer();
    		}
		}
    }
    
    /*public void waitForMouseClick()
	{
		//synchronized (lock2)
		//{
			System.out.println("Waiting for click!");
			
			try
			{
				lock2.wait();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		//}
	}*/
    
    private class Circle
    {
    	private int radius, cx, cy;
    	
    	public Circle()
    	{
    		this.radius = (int)(Math.random() * circleSize) + circleMinSize;
    		this.cx = randomX();
    		this.cy = randomY();
    	}
    	
    	public Circle(int cx, int cy)
    	{
    		this.radius = (int)(Math.random() * circleSize) + circleMinSize;
    		this.cx = cx;
    		this.cy = cy;
    	}
    	
    	public Circle(int radius, int cx, int cy)
    	{
    		this.radius = radius;
    		this.cx = cx;
    		this.cy = cy;
    	}
    	
    	public void drawCircle(GraphicsContext gc)
    	{
    		gc.setGlobalBlendMode(BlendMode.COLOR_DODGE);
    		gc.setFill(Color.rgb(randRGB(), randRGB(), randRGB()));
    		
    		int width = (int)(Math.random() * circleSize) + circleMinSize;
    		
    		gc.fillOval(cx - (radius / 2), cy - (radius / 2), width, width);
    		gc.setGlobalBlendMode(BlendMode.SRC_OVER); //reset
    		
    		CanvasRunner.updateImage();
    	}
    }
    
    private class Triangle
    {
    	private int v1x, v1y, v2x, v2y, v3x, v3y;
    	private Direction dir;
    	
    	public Triangle(int v1x, int v1y, int v2x, int v2y, int v3x, int v3y)
    	{
    		this.v1x = v1x;
    		this.v1y = v1y;
    		this.v2x = v2x;
    		this.v2y = v2y;
    		this.v3x = v3x;
    		this.v3y = v3y;
    	}
    	
    	public Triangle(int size, Triangle prev, int sharedVertices)
    	{
    		if (prev != null)
    		{
    			int rand = (int)(Math.random() * 3);

    			if (sharedVertices == 1)
    			{
	    			if (rand == 0)
	    			{
	    				this.v1x = prev.v1x;
	    				this.v1y = prev.v1y;
	    			}
	    			else if (rand == 1)
	    			{
	    				this.v1x = prev.v2x;
	    				this.v1y = prev.v2y;
	    			}
	    			else
	    			{
	    				this.v1x = prev.v3x;
	    				this.v1y = prev.v3y;
	    			}
    			}
    			else if (sharedVertices == 2)
    			{
    				if (rand == 0)
	    			{
	    				this.v1x = prev.v1x;
	    				this.v1y = prev.v1y;
	    				this.v2x = prev.v2x;
	    				this.v2y = prev.v2y;
	    			}
	    			else if (rand == 1)
	    			{
	    				this.v1x = prev.v2x;
	    				this.v1y = prev.v2y;
	    				this.v2x = prev.v3x;
	    				this.v2y = prev.v3y;
	    			}
	    			else
	    			{
	    				this.v1x = prev.v3x;
	    				this.v1y = prev.v3y;
	    				this.v2x = prev.v1x;
	    				this.v2y = prev.v1y;
	    			}
    			}
    		}
    		else
    		{
    			this.v1x = randomX();
    			this.v1y = randomY();

    			System.out.println("Prev is null!\n");
    		}

    		synchronized(lock)
    		{
    			int iterations = 0;
    			
	    		if (sharedVertices == 1)
	    		{
		    		do
		    		{
		    			v2x = v1x + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
		    			v2y = v1y + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
		    			iterations++;
		    		} while ((!(v2x > 0 && v2y > 0) || !(v2x < CanvasRunner.xDim && v2y < CanvasRunner.yDim) || Math.abs(v2x - v1x) < size / 2 || Math.abs(v2y - v1y) < size / 2) && occupiedCount < CanvasRunner.iterationCount);
		    		
		    		if (occupiedCount == iterationCount)
		    		{
		    			System.out.println("Max iterations!");
		    			waitForMouseClick = true;
		    			return;
		    		}
	    		}
	    		
	    		do
	    		{
	    			v3x = (v1x + v2x) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    			v3y = (v1y + v2y) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    			//v3x = v1x + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    			//v3y = v1y + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    		} while ((!(v3x > 0 && v3y > 0) || !(v3x < CanvasRunner.xDim && v3y < CanvasRunner.yDim) || CanvasRunner.isOccupied(v3x, v3y)) && occupiedCount < CanvasRunner.iterationCount); //|| Math.abs(v3x - ((v1x + v2x) / 2)) < 150 || Math.abs(v3y - ((v1y + v2y) / 2)) < 150);
    		
	    		if (occupiedCount == iterationCount)
	    		{
	    			//System.out.println("Max iterations!");
	    			waitForMouseClick = true;
	    		}
    		}
    	}
    	
    	public Triangle(int size, Triangle prev, Direction direction)
    	{
    		Direction currentDirection;
    		
    		if (direction == null)
    		{
    			direction = Direction.randomDirection();
    		}
    		
    		if (prev != null)
    		{
	    		int rand = (int)(Math.random() * 3);
	    		
	    		if (rand == 0)
	    		{
	    			this.v1x = prev.v1x;
	    			this.v1y = prev.v1y;
	    		}
	    		else if (rand == 1)
	    		{
	    			this.v1x = prev.v2x;
	    			this.v1y = prev.v2y;
	    		}
	    		else
	    		{
	    			this.v1x = prev.v3x;
	    			this.v1y = prev.v3y;
	    		}
    		}
    		else
    		{
    			this.v1x = randomX();
    			this.v1y = randomY();
    			
    			System.out.println("Prev is null!\n");
    		}
    		
    		//System.out.println("Rand was " + rand);
    		
    		do
    		{
	    		do
	    		{
	    			v2x = v1x + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    			v2y = v1y + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    		} while (!(v2x > 0 && v2y > 0) || !(v2x < CanvasRunner.xDim && v2y < CanvasRunner.yDim) || Math.abs(v2x - v1x) < 150 || Math.abs(v2y - v1y) < 150);
	
	    		do
	    		{
	    			v3x = (v1x + v2x) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    			v3y = (v1y + v2y) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    			//v3x = v1x + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    			//v3y = v1y + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
	    		} while (!(v3x > 0 && v3y > 0) || !(v3x < CanvasRunner.xDim && v3y < CanvasRunner.yDim)); //|| Math.abs(v3x - ((v1x + v2x) / 2)) < 150 || Math.abs(v3y - ((v1y + v2y) / 2)) < 150);
    		
	    		currentDirection = this.getDirection(prev);
    		} while (!(currentDirection == null) && !(currentDirection.equals(direction)));
    		
    		/*if (v1x == prev.v1x && v1y == prev.v1y)
    		{
    			System.out.println("Vert 1 is the same!");
    		}
    		else
    		{
    			System.out.println("This - " + v1x + ", " + v1y);
    			System.out.println("Prev - " + prev.v1x + ", " + prev.v1y);
    		}*/
    		
    		/*System.out.println("This - " + this);
    		System.out.println("Prev - " + prev);
    		System.out.println();*/
    	}
    	
    	public Triangle(int size, Triangle prev)
    	{
    		if (prev != null)
    		{
	    		int rand = (int)(Math.random() * 3);
	    		
	    		if (rand == 0)
	    		{
	    			this.v1x = prev.v1x;
	    			this.v1y = prev.v1y;
	    		}
	    		else if (rand == 1)
	    		{
	    			this.v1x = prev.v2x;
	    			this.v1y = prev.v2y;
	    		}
	    		else
	    		{
	    			this.v1x = prev.v3x;
	    			this.v1y = prev.v3y;
	    		}
    		}
    		else
    		{
    			this.v1x = randomX();
    			this.v1y = randomY();
    			
    			System.out.println("Prev is null!\n");
    		}
    		
    		//System.out.println("Rand was " + rand);
    		
    		do
    		{
    			v2x = v1x + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    			v2y = v1y + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    		} while (!(v2x > 0 && v2y > 0) || !(v2x < CanvasRunner.xDim && v2y < CanvasRunner.yDim) || Math.abs(v2x - v1x) < 150 || Math.abs(v2y - v1y) < 150);

    		do
    		{
    			v3x = (v1x + v2x) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    			v3y = (v1y + v2y) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    			//v3x = v1x + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    			//v3y = v1y + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    		} while (!(v3x > 0 && v3y > 0) || !(v3x < CanvasRunner.xDim && v3y < CanvasRunner.yDim)); //|| Math.abs(v3x - ((v1x + v2x) / 2)) < 150 || Math.abs(v3y - ((v1y + v2y) / 2)) < 150);
    		
    		/*if (v1x == prev.v1x && v1y == prev.v1y)
    		{
    			System.out.println("Vert 1 is the same!");
    		}
    		else
    		{
    			System.out.println("This - " + v1x + ", " + v1y);
    			System.out.println("Prev - " + prev.v1x + ", " + prev.v1y);
    		}*/
    		
    		/*System.out.println("This - " + this);
    		System.out.println("Prev - " + prev);
    		System.out.println();*/
    	}
    	
    	public Triangle(int size)
    	{
    		v1x = randomX();
    		v1y = randomY();

    		do
    		{
    			v2x = v1x + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    			v2y = v1y + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    		} while (!(v2x > 0 && v2y > 0) || !(v2x < CanvasRunner.xDim && v2y < CanvasRunner.yDim));

    		do
    		{
    			v3x = (v1x + v2x) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    			v3y = (v1y + v2y) / 2 + ((Math.random() > 0.5) ? (0 - (int)(Math.random() * size) + 20) : (int)(Math.random() * size) + 20);
    		} while (!(v3x > 0 && v3y > 0) || !(v3x < CanvasRunner.xDim && v3y < CanvasRunner.yDim));
    	}
    	
    	public Triangle()
    	{
    		v1x = randomX();
    		v1y = randomY();
    		
    		v2x = randomX();
    		v2y = randomY();
    		
    		v3x = randomX();
    		v3y = randomY();
    	}
    	
    	private Direction getDirection(Triangle prev)
    	{
    		if (prev == null)
    			this.dir = null;
    		
    		double averageThisX = average(v1x, v2x, v3x);
    		double averagePrevX = average(prev.v1x, prev.v2x, prev.v3x);
    		double averageThisY = average(v1y, v2y, v3y);
    		double averagePrevY = average(prev.v1y, prev.v2y, prev.v3y);
    		
    		if ((averageThisX - averagePrevX) > 100)
    		{
    			this.dir = Direction.RIGHT;
    		}
    		else if ((averageThisX - averagePrevX) < -100)
    		{
    			this.dir = Direction.LEFT;
    		}
    		else if ((averageThisY - averagePrevY) > 100)
    		{
    			this.dir = Direction.DOWN;
    		}
    		else if ((averageThisY - averagePrevY) < -100)
    		{
    			this.dir = Direction.UP;
    		}
    		else
    		{
    			this.dir = Direction.randomDirection();
    		}
    		
    		return dir;
    	}
    	
    	public Direction getSavedDirection()
    	{
    		return dir;
    	}
    	
    	private double average(double ... doubles)
    	{
    		double avr = 0;
    		
    		for (double x : doubles)
    			avr += x;
    		
    		return avr / doubles.length;
    	}
    	
    	public void drawTriangle(GraphicsContext gc)
    	{
    		gc.setFill(Color.rgb(randRGB(), randRGB(), randRGB()));
    		
    		gc.fillPolygon(new double[] { v1x,  v2x,  v3x },
    						new double[] { v1y, v2y, v3y },
    						3);
    		gc.strokePolygon(new double[] { v1x,  v2x,  v3x },
					new double[] { v1y, v2y, v3y },
					3);
    		
    		CanvasRunner.updateImage();
    	}
    	
    	public String toString()
    	{
    		return "(" + v1x + ", " + v1y + "), (" + v2x + ", " + v2y + "), (" + v3x + ", " + v3y + ")";
    	}
    }
}
