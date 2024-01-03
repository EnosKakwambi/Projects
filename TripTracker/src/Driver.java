import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * @author enoskakwambi
 * @created 05/26/2023
 */
public class Driver {
	
	// Declare class data
	private static boolean enableStops = false;
	
	private static Timer timer;
	private static JFrame mainFrame;
	private static JPanel topPanel  = new JPanel();
	
	private static final int zoomLevel = 6;
	private static final int dynamicZoom = 8;
	
	private static JButton playButton = new JButton("Play");
	private static JButton resetButton = new JButton("Clear");
	private static JCheckBox includeStops = new JCheckBox("Include Stops");
	private static String [] seconds = {"Animation Time", "15", "30", "60", "90"};
	private static JComboBox <String> times = new JComboBox <String>(seconds);
	
	private static JMapViewer mapView = new JMapViewer();
	private static JCheckBox useDots = new JCheckBox("Use dots instead");
	private static boolean dots = false;
	
	private static final String DEFAULT_TRIP_FILE = "triplog.csv";
	private static String tripLogFile = null;
	private static JButton selectTripLogFile = new JButton("Select Trip Log");
	
	private static final String DEFAULT_IMAGE_FILE = "arrow.png";
	private static String imageFile = DEFAULT_IMAGE_FILE;
	private static JButton selectImageFile = new JButton("Select Tracker Image");
	private static JButton pauseButton = new JButton("Pause");
	private static AtomicBoolean isPaused = new AtomicBoolean(false);
	private static JProgressBar progressBar = new JProgressBar(0, 100);

	
	
	
    public static void main(String[] args) throws FileNotFoundException, IOException {
    	
    	// Set up frame, include your name in the title
    	mainFrame = new JFrame();
    	mainFrame.setSize(1200, 700);
    	mainFrame.setLayout(new BorderLayout());
    	mainFrame.setTitle("TripTracker");
    	
    	// Set up Panel for input selections
        topPanel.setLayout(new FlowLayout());
        topPanel.setBounds(0,0, 1200, 50);
        topPanel.setBackground(Color.DARK_GRAY);
        mainFrame.add(topPanel, BorderLayout.NORTH);
        
        // Visual effects to buttons to make them clearer
        playButton.setFocusable(false);
        includeStops.setFocusable(false);
        times.setFocusable(false);
        useDots.setFocusable(false);
        selectTripLogFile.setFocusable(false);
        selectImageFile.setFocusable(false);
        pauseButton.setFocusable(false);
        progressBar.setStringPainted(true);
        resetButton.setFocusable(false);
        
        //make sure the reset button is disabled
        resetButton.setEnabled(false);
        
        // Add all to top panel
        topPanel.add(times);
        topPanel.add(includeStops);
        topPanel.add(playButton);
        topPanel.add(pauseButton);
        topPanel.add(useDots);
        topPanel.add(selectTripLogFile);
        topPanel.add(selectImageFile);
        topPanel.add(resetButton);
        topPanel.add(progressBar);
        
        // Set up mapViewer
        mapView.setTileSource(new OsmTileSource.TransportMap());
        mapView.setPreferredSize(new Dimension(1200,600));
        mainFrame.getContentPane().add(mapView, BorderLayout.CENTER);
        
        // Add listeners for GUI components
        resetButton.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		//make the reset button unclickable
        		resetButton.setEnabled(false);
        		//set the pause/resume button back to pause
        		pauseButton.setText("Pause");
                isPaused.set(false);
                //reset all buttons and end the animation.
                progressBar.setValue(0);
            	resetMap();
        		resetButtons();
				timer.cancel();
        	}
        });
        
        playButton.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		playButton.setEnabled(false);
        		timer = new Timer();
        		try
        		{
        			animateTrip(enableStops, timer, times, mapView, mainFrame);
				}
        		catch (FileNotFoundException e1)
        		{
					
					e1.printStackTrace();
				} 
        		catch (IOException e1)
        		{
					
					e1.printStackTrace();
				}
        	}
        	
        });
        
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isPaused.get()) {
                    pauseButton.setText("Pause");
                    isPaused.set(false);
                    resetButton.setEnabled(false);
                } else {
                    pauseButton.setText("Resume");
                    isPaused.set(true);
                    resetButton.setEnabled(true);
                }
            }
        });
        
        selectTripLogFile.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		JFileChooser fileChooser = new JFileChooser();
        		FileNameExtensionFilter csv = new FileNameExtensionFilter("*.csv","csv");
        		fileChooser.addChoosableFileFilter(csv);
        		fileChooser.setCurrentDirectory(new File("."));
        		fileChooser.setFileFilter(csv);
        		int response = fileChooser.showOpenDialog(null);
        		if (response == JFileChooser.APPROVE_OPTION)
        		{
        			tripLogFile = fileChooser.getSelectedFile().getAbsolutePath();
        		}
        		else
        		{
        			tripLogFile = DEFAULT_TRIP_FILE;
        		}
        	}
        	
        });
        
        selectImageFile.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		JFileChooser imageChoose = new JFileChooser();
        		FileNameExtensionFilter png = new FileNameExtensionFilter("*.png","png");
        		imageChoose.addChoosableFileFilter(png);
        		imageChoose.setCurrentDirectory(new File("."));
        		imageChoose.setFileFilter(png);
        		int response = imageChoose.showOpenDialog(null);
        		if(response == JFileChooser.APPROVE_OPTION)
        		{
        			imageFile = imageChoose.getSelectedFile().getAbsolutePath();
        		}
        		else
        		{
        			imageFile = DEFAULT_IMAGE_FILE;
        		}
        	}
        });
        
        includeStops.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		includeStops.setEnabled(true);
        		enableStops = includeStops.isSelected();
        	}
        	
        });
        
        useDots.addActionListener(new ActionListener()
        {
        	@Override
        	public void actionPerformed(ActionEvent e)
        	{
        		dots = true;
        	}
        });
        
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	mainFrame.setVisible(true);
    }
    
    public static ArrayList <ArrayList <Double>> getCoordinates (ArrayList <TripPoint> trip)
    {
    	ArrayList <ArrayList <Double>> temp = new ArrayList <ArrayList<Double>>();
    	
    	//add Latitude ArrayList
    	temp.add(new ArrayList <Double>());
    	//add Longitude ArrayList
    	temp.add(new ArrayList <Double>());
    	
    	for (int k = 0; k < trip.size(); k++)
    	{
    		temp.get(0).add(trip.get(k).getLat());
    		temp.get(1).add(trip.get(k).getLon());
    	}
    	
    	ArrayList <ArrayList <Double>> coordinates = new ArrayList <ArrayList<Double>>(temp);
		return coordinates;
    	
    }
    
    public static void animateTrip(boolean stopsEnabled, Timer timer, JComboBox <String> chosenTime, JMapViewer map, JFrame frame) throws FileNotFoundException, IOException
    {
    	progressBar.setValue(0);
    	resetMap();
    	
    	// Read file and call stop detection
    	if(tripLogFile == null)
    	{
    		JOptionPane.showMessageDialog(mainFrame, "Select Trip Log!", "Warning!", JOptionPane.INFORMATION_MESSAGE);
    		resetButtons();
    		return;
    	}
    	else
    	{
    		TripPoint.readFile(tripLogFile);
    	}
    	TripPoint.h2StopDetection();
    	BufferedImage image;
    	
    	times.setEnabled(false);
    	includeStops.setEnabled(false);
    	useDots.setEnabled(false);
    	selectTripLogFile.setEnabled(false);
    	selectImageFile.setEnabled(false);
    	
    	image = ImageIO.read(new File(imageFile));
    	ArrayList <ArrayList<Double>> coordinates;
    	
    	if (enableStops)
    	{
    		coordinates = getCoordinates(TripPoint.getTrip());
    	}
    	else
    	{
    		coordinates = getCoordinates(TripPoint.getMovingTrip());
    	}
    	
    	TimerTask tTask = new TimerTask()
    	{
    		int counter = 0;
    		Coordinate currentLocation;
    		Coordinate lastLocation;
    		IconMarker currentMarker;
    		IconMarker lastMarker;
    		
    		
    		@Override
    		public void run()
    		{
    			if(counter < (coordinates.get(0).size() - 1))
    			{
    				progressBar.setValue((int) (((float) counter / coordinates.get(0).size()) * 100));
        			dynamicCenter(coordinates, counter);
        			
        			if (isPaused.get())
        			{
        	            return;
        			}
        			
    				if(counter == 0)
    				{
	    				currentLocation = new Coordinate(coordinates.get(0).get(counter), coordinates.get(1).get(counter));
	    				currentMarker = new IconMarker(currentLocation, 0.5, image);
	    				mapView.addMapMarker(currentMarker);
	    				++counter;
    				}
    				else
    				{
    					lastLocation = currentLocation;
    					lastMarker = currentMarker;
    					currentLocation = new Coordinate(coordinates.get(0).get(counter), coordinates.get(1).get(counter));
    					BufferedImage rotatedImage = image;
						try
						{
							rotatedImage = rotateImage(image, calculateAngle(lastLocation, currentLocation));
						}
						catch (IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					currentMarker = new IconMarker(currentLocation, 0.5, rotatedImage);
    					mapView.removeMapMarker(lastMarker);
    					mapView.addMapMarker(currentMarker);
    					if(dots) { drawDots(lastLocation);}
    					else { drawLine(lastLocation, currentLocation);}
    					++counter;
    				}
    			}
    			else
    			{
    				setCenter(coordinates);
    				progressBar.setValue(100);
    				resetButtons();
    				resetButton.setEnabled(true);
    				timer.cancel();
    				return;
    			}
    		}
    	};
    	
    	long period = 10000/coordinates.get(0).size();
    	if(times.getSelectedIndex() == 0)
    	{
    		JOptionPane.showMessageDialog(mainFrame, "Select Animation Time!", "Warning!", JOptionPane.INFORMATION_MESSAGE);
    		resetButtons();
    		return;
    	}
    	else if(times.getSelectedIndex() == 1)
    	{
    		period = 15000/coordinates.get(0).size();
    	}
    	else if(times.getSelectedIndex() == 2)
    	{
    		period = 30000/coordinates.get(0).size();
    	}
    	else if(times.getSelectedIndex() == 3)
    	{
    		period = 60000/coordinates.get(0).size();
    	}
    	else if(times.getSelectedIndex() == 4)
    	{
    		period = 90000/coordinates.get(0).size();
    	}
    	
    	timer.scheduleAtFixedRate(tTask, 0, period);
    }
    
    public static void drawLine(Coordinate last, Coordinate current)
    {
    	MapPolygonImpl polygon = new MapPolygonImpl(last, current, last);
    	polygon.setColor(Color.RED);
    	polygon.setBackColor(Color.BLACK);
    	polygon.setStroke(new BasicStroke(2f));
    	mapView.addMapPolygon(polygon);
    }
    
    public static void drawDots(Coordinate last)
    {
    	MapMarkerDot dot = new MapMarkerDot(last);
    	dot.setColor(Color.BLACK);
    	dot.setBackColor(Color.RED);
    	
    	mapView.addMapMarker(dot);
    }
    
    public static void resetMap()
    {
    	playButton.setEnabled(false);
    	mapView.removeAllMapPolygons();
    	mapView.removeAllMapMarkers();
    }
    
    public static void resetButtons() {
        times.setEnabled(true);
        includeStops.setEnabled(true);
        useDots.setEnabled(true);
        playButton.setEnabled(true);
        selectTripLogFile.setEnabled(true);
        selectImageFile.setEnabled(true);
    }
    
    public static void setCenter(ArrayList<ArrayList<Double>> coords)
    {
    	double minLat = Collections.min(coords.get(0));
    	double maxLat = Collections.max(coords.get(0));
    	double minLon = Collections.min(coords.get(1));
    	double maxLon = Collections.max(coords.get(1));
    	ICoordinate center = new Coordinate((maxLat + minLat)/2.0, (maxLon + minLon)/2.0);
    	mapView.setDisplayPosition(center, zoomLevel);
    	return;
    	
    }
    
    public static void dynamicCenter(ArrayList<ArrayList<Double>> coords, int counter)
    {
    	double minLat = coords.get(0).get(counter);
    	double maxLat = coords.get(0).get((counter + 1));
    	double minLon = coords.get(1).get(counter);
    	double maxLon = coords.get(1).get((counter + 1));
    	
    	ICoordinate center = new Coordinate((maxLat + minLat)/2.0, (maxLon + minLon)/2.0);
    	mapView.setDisplayPosition(center, dynamicZoom);
    	
    	return;
    }
    
    public static double calculateAngle(Coordinate last, Coordinate current)
    {
    	double currentLat = current.getLat();
    	double currentLon = current.getLon();
    	double lastLat = last.getLat();
    	double lastLon = last.getLon();
    	
    	double latDiff = Math.toRadians(currentLat) - Math.toRadians(lastLat);
    	double lonDiff = Math.toRadians(currentLon) - Math.toRadians(lastLon);
    	
    	double angle = Math.atan2(lonDiff, latDiff);
    	return Math.toDegrees(angle);
    }
    
    public static BufferedImage rotateImage(BufferedImage image, double angle) throws IOException {
    	
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage rotatedImage = ImageIO.read(new File (imageFile));
        Graphics2D graphics = rotatedImage.createGraphics();
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(angle), width / 2.0, height / 2.0);
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage rotated = op.filter(rotatedImage, null);
        graphics.drawImage(image, transform, null);
        graphics.dispose();

        return rotated;
    }
    
}