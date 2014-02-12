package rover.control;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
//import ioio.lib.util.AbstractIOIOActivity;
import ioio.lib.util.android.IOIOActivity;
import ioio.lib.android.bluetooth.*;
import net.mitchtech.ioio.servocontrol.R;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import java.net.UnknownHostException;
import java.io.IOException;

import rover.netclient.IPIDClient;
import rover.netclient.TCPNetClient;
import rover.netclient.UDPNetClient;

//public class ServoControlActivity extends AbstractIOIOActivity {
public class ServoControlActivity extends IOIOActivity implements SensorEventListener, LocationListener {
	public final static int NOCMD = 0;
	public final static int FORWARD = 1;
	public final static int BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int STOP = 5;
	public final static int MANUAL = -1;
	public volatile int command = MANUAL;
	
	private final int MOTOR1 = 11;
	private final int MOTOR2 = 12;
	private final int MOTOR3 = 13;
	private final int MOTOR4 = 14;
	private final int DIRECTION1 = 3;
	private final int DIRECTION2 = 6;
	private final int DIRECTION3 = 7;
	private final int DIRECTION4 = 10;
	private final int PWM_FREQ = 100;
	private final int SENSOR1 = 35;
	
	private int SPEED = 1500;
	
	private int SERVER_PORT = 5000;
	private String SERVER_IP = "192.168.1.4";
	
	private Button bForward;
	private Button bBackward;
	private Button bLeft;
	private Button bRight;
	
	private ToggleButton tMotor1;
	private ToggleButton tMotor2;
	private ToggleButton tMotor3;
	private ToggleButton tMotor4;
	
	private SeekBar sBar;
	
	private TextView txtViewSensor1;
	private static final ExecutorService singleClientPool = Executors.newSingleThreadExecutor();
	
	private UDPNetClient clientLoop = new UDPNetClient(this);
	private static float sensors[];
	
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;
	
	static volatile float azimut = 0.0f;
	static volatile float azimutPre = 0.0f;
	static final float azimutGain = 0.7f;
	public static volatile int avgAzimut = 0;
	
	
	private LocationManager locationManager;
	
	public static double android_GPS_Lat = 0;
	public static double android_GPS_Lon = 0;
	public static double skyhook_GPS_Lat = 0;
	public static double skyhook_GPS_Lon = 0;
	
	
	public static ToggleButton togSkyHookAccuracy;
	public static TextView txtAndroidGPS;
	private TextView txtSkyHookGPS;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		bForward = (Button) findViewById(R.id.btnForward);
		bBackward = (Button) findViewById(R.id.btnBackward);
		bLeft = (Button) findViewById(R.id.btnLeft);
		bRight = (Button) findViewById(R.id.btnRight);
		
		tMotor1 = (ToggleButton) findViewById(R.id.tgMotor1);
		tMotor2 = (ToggleButton) findViewById(R.id.tgMotor2);
		tMotor3 = (ToggleButton) findViewById(R.id.tgMotor3);
		tMotor4 = (ToggleButton) findViewById(R.id.tgMotor4);
		
		sBar = (SeekBar) findViewById(R.id.seekBar1);
		
		txtViewSensor1 = (TextView) findViewById(R.id.txtVoltage);
		sensors = new float[5];
		//new IMU().start();
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    	accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    	mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
	    mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
	    
	    
	    
	    /** Android GPS 
	     *  Gps Location Service LocationManager Object 
	     */
	    txtAndroidGPS = (TextView) findViewById(R.id.androidGPS);
	    
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                0,   // Immediate
                0, this);
        
        /** SkyHook GPS
         *
         */
        txtSkyHookGPS = (TextView) findViewById(R.id.skyHookGPS);
        togSkyHookAccuracy = (ToggleButton) findViewById(R.id.skyHookTogButton);
	    
	    
	    
		try {
        	boolean res = clientLoop.serverConnect(SERVER_IP, SERVER_PORT);
        	if(res) {
        		singleClientPool.execute(clientLoop);
        	}
        }
        catch(final UnknownHostException ex) {
        	runOnUiThread(new Runnable(){
   		 		@Override
   		 		public void run() {
   		 			// User is at the waypoint. Display an alert toast for a few seconds
   		 			Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
   		 		}
   		 	});
        }
        catch(final IOException ex) {
        	runOnUiThread(new Runnable(){
   		 		@Override
   		 		public void run() {
   		 			// User is at the waypoint. Display an alert toast for a few seconds
   		 			Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
   		 		}
   		 	});
        }
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		enableUi(false);
	}

//	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
	class IOIOThread extends BaseIOIOLooper{
		private PwmOutput pwmMotor1;
		private PwmOutput pwmMotor2;
		private PwmOutput pwmMotor3;
		private PwmOutput pwmMotor4;
		
		private DigitalOutput direction1;
		private DigitalOutput direction2;
		private DigitalOutput direction3;
		private DigitalOutput direction4;
		
		private AnalogInput sensor1;

		public void setup() throws ConnectionLostException {
			try {
				pwmMotor1 = ioio_.openPwmOutput(MOTOR1, PWM_FREQ);
				pwmMotor2 = ioio_.openPwmOutput(MOTOR2, PWM_FREQ);
				pwmMotor3 = ioio_.openPwmOutput(MOTOR3, PWM_FREQ);
				pwmMotor4 = ioio_.openPwmOutput(MOTOR4, PWM_FREQ);
				
				direction1 = ioio_.openDigitalOutput(DIRECTION1, true);
				direction2 = ioio_.openDigitalOutput(DIRECTION2, true);
				direction3 = ioio_.openDigitalOutput(DIRECTION3, true);
				direction4 = ioio_.openDigitalOutput(DIRECTION4, true);
				
				sensor1 = ioio_.openAnalogInput(SENSOR1);
				enableUi(true);
				
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}
		
		public void setText(final String msg){
			runOnUiThread(new Runnable(){
				@Override
				public void run(){
					txtViewSensor1.setText(msg);	
				}
			});
		}

public void loop() throws ConnectionLostException {
			
			updateGPS_UI();
			
			command = clientLoop.cmd;
			if(command == MANUAL) {		// MANUAL
				SPEED = sBar.getProgress();
				try {
					setText("" + sensor1.read());
					if(bForward.isPressed() || bBackward.isPressed()) {
						
						// Manual Forward
						if(bForward.isPressed()){
							direction1.write(true);
							direction2.write(false);
							direction3.write(true);
							direction4.write(false);
						} 
						else {	// Manual Backward
							direction1.write(false);
							direction2.write(true);
							direction3.write(false);
							direction4.write(true);
						}
						//pulseWidth range 0 - 1500
						if(tMotor1.isChecked()) pwmMotor1.setPulseWidth(SPEED);
						if(tMotor2.isChecked()) pwmMotor2.setPulseWidth(SPEED);
						if(tMotor3.isChecked()) pwmMotor3.setPulseWidth(SPEED);
						if(tMotor4.isChecked()) pwmMotor4.setPulseWidth(SPEED);
					}
					else if(bLeft.isPressed() || bRight.isPressed()) {
						
						
						// Manual Left				
						if(bLeft.isPressed()){
							direction1.write(false);
							direction2.write(true);
							direction3.write(true);
							direction4.write(false);
						}
						else { 	// Manual Right
							direction1.write(true);
							direction2.write(false);
							direction3.write(false);
							direction4.write(true);	
						}
						//pulseWidth range 0 - 1500
						if(tMotor1.isChecked()) pwmMotor1.setPulseWidth(SPEED);
						if(tMotor2.isChecked()) pwmMotor2.setPulseWidth(SPEED);
						if(tMotor3.isChecked()) pwmMotor3.setPulseWidth(SPEED);
						if(tMotor4.isChecked()) pwmMotor4.setPulseWidth(SPEED);
					}
					else {	// Stop
						pwmMotor1.setPulseWidth(0);
						pwmMotor2.setPulseWidth(0);
						pwmMotor3.setPulseWidth(0);
						pwmMotor4.setPulseWidth(0);
					}
				Thread.sleep(10);
			} catch (InterruptedException e) {
				ioio_.disconnect();
			} catch (ConnectionLostException e) {
				enableUi(false);
				throw e;
			}
		}
		else {	// REMOTE
			
			try {
				setText("" + sensor1.read());
				if(command == FORWARD || command == BACKWARD){
					if(command == FORWARD) {
						
						// Server Command - Forward
						direction1.write(true);
						direction2.write(false);
						direction3.write(true);
						direction4.write(false);
					}
					else {
						// Server Command - Backward
						direction1.write(false);
						direction2.write(true);
						direction3.write(false);
						direction4.write(true);
					}
					//pulseWidth range 0 - 1500
					if(tMotor1.isChecked()) pwmMotor1.setPulseWidth(SPEED);
					if(tMotor2.isChecked()) pwmMotor2.setPulseWidth(SPEED);
					if(tMotor3.isChecked()) pwmMotor3.setPulseWidth(SPEED);
					if(tMotor4.isChecked()) pwmMotor4.setPulseWidth(SPEED);
				}
				else if(command == LEFT || command == RIGHT) {
					
					// Server Command - Left
					if(command == LEFT) {
					direction1.write(false);
					direction2.write(true);
					direction3.write(true);
					direction4.write(false);
					}
					else {	// Server Command - Right
						direction1.write(true);
						direction2.write(false);
						direction3.write(false);
						direction4.write(true);	
					}
					
					if(tMotor1.isChecked()) pwmMotor1.setPulseWidth(SPEED);
					if(tMotor2.isChecked()) pwmMotor2.setPulseWidth(SPEED);
					if(tMotor3.isChecked()) pwmMotor3.setPulseWidth(SPEED);
					if(tMotor4.isChecked()) pwmMotor4.setPulseWidth(SPEED);
				}
				else if (command == STOP) {	// Stop
					pwmMotor1.setPulseWidth(0);
					pwmMotor2.setPulseWidth(0);
					pwmMotor3.setPulseWidth(0);
					pwmMotor4.setPulseWidth(0);
				}
			Thread.sleep(10);
		} catch (InterruptedException e) {
			ioio_.disconnect();
		} catch (ConnectionLostException e) {
			enableUi(false);
			throw e;
		}
		}
	 }	
	}
	
	@Override
//	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
	protected IOIOLooper createIOIOLooper() {
		return new IOIOThread();
	}

	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				bForward.setEnabled(enable);
				bBackward.setEnabled(enable);
			}
		});
	}
	
	public void btnFordwardonTouch(View v, MotionEvent e){
		
	}
	
	public void btnBackwardonTouch(View v, MotionEvent e){
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public static float getSensorReadings(int indx){
		return sensors[indx];
	}
	static boolean compFirstRun = true;
	static float[] azimBuf = new float[10];
	float[] mGravity;
	float[] mGeomagnetic;
	public void onSensorChanged(final SensorEvent event) {
		sensors[0] = event.values[0];
		sensors[1] = event.values[1];
		sensors[2] = event.values[2];
           if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            		mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            		mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
            		float R[] = new float[9];
            		float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            		if (success) {
            			float orientation[] = new float[3];
            			SensorManager.getOrientation(R, orientation);
            			azimut = orientation[0]; // orientation contains: azimut, pitch and roll
            			if(compFirstRun) {
            				compFirstRun = false;
            				for(int i = 0; i<azimBuf.length; i++){
            					azimBuf[i] = azimut; 
            				}
            			}
            			else {
            				for(int i=0; i< (azimBuf.length - 1); i++) {
            					azimBuf[i] = azimBuf[i+1];
            				}
            				azimBuf[azimBuf.length - 1] = azimut;
            			}
            			float avg = 0.0f;
            			for(float x: azimBuf){
        					avg += x; 
        				}
            			avg *= 0.1f;
            			avgAzimut = (int)Math.toDegrees(avg);
            			//azimut = azimutPre*azimutGain + azimut*(1-azimutGain);
            			//azimutPre = azimut;
                		//tv_current_comp_course.setText(String.valueOf(avgAzimut) + " ");
                		
            	}
            }
	   
	}
	
    public void updateGPS_UI() {
    	
    	
    	runOnUiThread(new Runnable(){
		 		@Override
		 		public void run() {
		 			txtAndroidGPS.setText("Android GPS: lat " + android_GPS_Lat + ", " + android_GPS_Lon);

		 			txtSkyHookGPS.setText("SkyHook GPS: lat " + skyhook_GPS_Lat + ", " + skyhook_GPS_Lon);
		 		}
		 	});
    	
    }
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		try {
			boolean res = clientLoop.serverConnect(SERVER_IP, SERVER_PORT);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    
	/**  Android GPS Required Methods */

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		android_GPS_Lat = location.getLatitude();
		android_GPS_Lon = location.getLongitude();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	public void setSpeed(int s) {
		
		/*if(clientLoop.cmd != MANUAL) {
			SPEED = s;
		}*/
	}
	
	
}