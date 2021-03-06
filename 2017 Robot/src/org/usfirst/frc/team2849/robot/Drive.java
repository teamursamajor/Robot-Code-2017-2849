package org.usfirst.frc.team2849.robot;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.Spark;

public class Drive implements Runnable {

	// NICK YOU SHOULD CLEAN UP THIS CODE THANKS
	// ALSO GET RID OF THE YELLOW TRIANGLES THANKS AGAIN NICK

	/**
	 * can't have Sparks and RobotDrive inside another robot drive you would
	 * have to dig in to find out why, but that's the issue
	 * 
	 * - other charlie
	 */
	private static AHRS ahrs;

	private static double xaxis = 0.0;
	private static double yaxis = 0.0;
	private static double zaxis = 0.0;
	private static double angle = 0.0;
	private static double currentAngle = 0.0;

	private Spark frontLeftMotor2;
	private Spark frontRightMotor1;
	private Spark frontRightMotor2;
	private Spark backLeftMotor1;
	private Spark backLeftMotor2;
	private Spark backRightMotor1;
	private Spark backRightMotor2;
	private int numMotors;
	private Spark frontLeftMotor1;

	private Boolean threadLock = false;

	private boolean headless = true;

	private double headingOffset = 0.0;

	public Ultrasonic ultra = new Ultrasonic(0, 7);
	private static boolean autoDrive = false;

	//private static LogitechFlightStick joy;

	private final double STOPPING_DISTANCE = 8.0;
//0 fl 1 fr 9 bl 8 br
	/**
	 * Drive constructor for 4-motor drive.
	 * 
	 * @param t1
	 *            Port of the front left motor.
	 * @param t2
	 *            Port of the back left motor.
	 * @param t3
	 *            Port of the front right motor.
	 * @param t4
	 *            Port of the back right motor.
	 */
	public Drive(int t1, int t2, int t3, int t4, AHRS ahrs, LogitechFlightStick joy) {

		frontLeftMotor1 = new Spark(t1);
		backLeftMotor1 = new Spark(t2);
		frontRightMotor1 = new Spark(t3);
		backRightMotor1 = new Spark(t4);
		frontRightMotor1.setInverted(true);
		backRightMotor1.setInverted(true);
		numMotors = 4;
		Drive.ahrs = ahrs;
		//Drive.joy = joy;
	}

	/**
	 * Drive constructor for 8-motor drive.
	 *
	 * @param t1
	 *            Port of the front left full cim.
	 * @param t2
	 *            Port of the front left mini cim.
	 * @param t3
	 *            Port of the front right full cim.
	 * @param t4
	 *            Port of the front right mini cim.
	 * @param t5
	 *            Port of the back left full cim.
	 * @param t6
	 *            Port of the back left mini cim.
	 * @param t7
	 *            Port of the back right full cim.
	 * @param t8
	 *            Port of the back right mini cim.
	 */
	public Drive(int t1, int t3, int t2, int t4,

			int t5, int t7, int t6, int t8, AHRS ahrs, LogitechFlightStick joy) {

		frontLeftMotor1 = new Spark(t1);
		frontLeftMotor2 = new Spark(t2);
		frontRightMotor1 = new Spark(t3);
		frontRightMotor2 = new Spark(t4);
		backLeftMotor1 = new Spark(t5);
		backLeftMotor2 = new Spark(t6);
		backRightMotor1 = new Spark(t7);
		backRightMotor2 = new Spark(t8);
		numMotors = 8;
		Drive.ahrs = ahrs;
		//Drive.joy = joy;
	}

	/**
	 * Normalize all wheel speeds if the magnitude of any wheel is greater than
	 * 1.0.
	 */
	protected void normalize(double[] wheelSpeeds) {
		double maxMagnitude = Math.abs(wheelSpeeds[0]);
		for (int i = 1; i < numMotors; i++) {
			double temp = Math.abs(wheelSpeeds[i]);
			if (maxMagnitude < temp) {
				maxMagnitude = temp;
			}
		}
		if (maxMagnitude > 1.0) {
			for (int i = 0; i < numMotors; i++) {
				wheelSpeeds[i] = wheelSpeeds[i] / maxMagnitude;
			}
		}
	}

	/**
	 * Rotate a vector in Cartesian space.
	 * 
	 * @param x
	 *            X coordinate to be rotated
	 * @param y
	 *            Y coordinate to be rotated
	 * @param angle
	 *            angle in degrees to be rotated by
	 *
	 *            Derived via the relationship [Rotation Matrix]*[x; y] =
	 *            [rotated vector] Where [Rotation Matrix] = [cos(angle)
	 *            -sin(angle)] [sin(angle) cos(angle)]
	 */
	protected double[] rotateVector(double x, double y, double angle) {
		double cosA = Math.cos(angle * (Math.PI / 180.0));
		double sinA = Math.sin(angle * (Math.PI / 180.0));
		double[] out = new double[2];
		out[0] = x * cosA - y * sinA;
		out[1] = x * sinA + y * cosA;
		return out;
	}

	/**
	 * This will drive the robot in omnidirectional holonomic drive. Do NOT call
	 * this method from Robot.java! Call drive.drive() instead!
	 * 
	 * @param xaxis
	 *            The x axis of the joystick.
	 * @param yaxis
	 *            The y axis of the joystick.
	 * @param raxis
	 *            The rotation of the joystick.
	 * @param gyroAngle
	 *            The input of the gyro.
	 * 
	 */
	private void mecanumDrive(double xaxis, double yaxis, double raxis, double gyroAngle) {

		double xIn = xaxis;
		double yIn = yaxis;
		// Negate y for the joystick.
		yIn = -yIn;
		raxis = -raxis;
		// Compensate for gyro angle.
		double[] rotated = rotateVector(xIn, yIn, gyroAngle);
		xIn = rotated[0];
		yIn = rotated[1];

		if (numMotors == 4) {
			// had to change new double[numMotors] to 14 so we can use
			// motor #s > 4
			double[] wheelSpeeds = new double[14];
			wheelSpeeds[0] = xIn + yIn + raxis;
			wheelSpeeds[9] = -xIn + yIn - raxis;
			wheelSpeeds[8] = -xIn + yIn + raxis;
			wheelSpeeds[1] = xIn + yIn - raxis;

			normalize(wheelSpeeds);
			frontLeftMotor1.set(wheelSpeeds[0]);
			frontRightMotor1.set(wheelSpeeds[9]);
			backLeftMotor1.set(wheelSpeeds[8]);
			backRightMotor1.set(wheelSpeeds[1]);

		} else {
			double[] wheelSpeeds = new double[numMotors];
			wheelSpeeds[0] = xIn + yIn + raxis;
			wheelSpeeds[1] = xIn + yIn + raxis;
			wheelSpeeds[2] = -xIn + yIn - raxis;
			wheelSpeeds[3] = -xIn + yIn - raxis;
			wheelSpeeds[4] = -xIn + yIn + raxis;
			wheelSpeeds[5] = -xIn + yIn + raxis;
			wheelSpeeds[6] = xIn + yIn - raxis;
			wheelSpeeds[7] = xIn + yIn - raxis;

			normalize(wheelSpeeds);
			frontLeftMotor1.set(wheelSpeeds[0]);
			frontLeftMotor2.set(wheelSpeeds[1]);
			frontRightMotor1.set(wheelSpeeds[2]);
			frontRightMotor2.set(wheelSpeeds[3]);
			backLeftMotor1.set(wheelSpeeds[4]);
			backLeftMotor2.set(wheelSpeeds[5]);
			backRightMotor1.set(wheelSpeeds[6]);
			backRightMotor2.set(wheelSpeeds[7]);
		}

	}

	/**
	 * Drives the robot in a direction without a stop.
	 * 
	 * @param angleDeg
	 *            An angle measurement in degrees.
	 */
	public void driveDirection(double angleDeg) {
		drive(0, 0.5, 0, -angleDeg);

	}

	/**
	 * This will drive the robot in a direction for the specified time.
	 * 
	 * @param angleDeg
	 *            An angle measurement in degrees.
	 * @param time
	 *            A time measurement in milliseconds.
	 */
	public void driveDirection(double angleDeg, int time) {
		double timer = System.currentTimeMillis();
		drive(0, -.5, 0, -angleDeg);

		while (System.currentTimeMillis() - timer < time) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		drive(0, 0, 0, 0);
	}

	/**
	 * This will turn the robot at a steady rate clockwise until it is pointing
	 * at the specified angle.
	 * 
	 * @param angleDeg
	 *            An angle measurement in degrees,
	 */
	public void driveAngle(double angleDeg) {

		mecanumDrive(0, 0, .5, 0);
		if (getHeading() == angleDeg) {
			mecanumDrive(0, 0, 0, 0);
		}

	}

	/**
	 * This will drive the robot the specified distance at the specified angle.
	 * 
	 * @param distance
	 *            A distance in meters.
	 * @param angleDeg
	 *            An angle measurement in degrees.
	 */
	public void mechDriveDistance(double distance, double angleDeg) { // in
																		// meters

		double displacement = 0;
		ahrs.resetDisplacement();

		driveDirection(angleDeg);
		long time = System.currentTimeMillis();
		while (displacement <= distance) {
			displacement += Math.abs(Math.hypot(ahrs.getRawAccelX() * 9.81, ahrs.getRawAccelZ() * 9.81) * .5
					* Math.pow((System.currentTimeMillis() - time) / 1000, 2));
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		drive(0, 0, 0, 0);
	}

	/**
	 * Runs automatically after calling startDrive(). Will continue running in
	 * the drive thread while the robot is on. Place all calls to drive code
	 * inside the while loop.
	 */
	public void run() {
		while (true) {
			// currentAngle = getHeading();
			// angleLock(joy.getAxisGreaterThan(0, 0.1),
			// joy.getAxisGreaterThan(1, 0.1), joy.getAxisGreaterThan(2, 0.1),
			// currentAngle);
			if (autoDrive) {
				// currentAngle = getHeading();
				// angleLock();
				// System.out.println("Distance: " + ultra.getDistance());
				// System.out.println("Voltage: " + ultra.getVoltage());
				// checking in centimeters
				if (ultra.getDistance() < STOPPING_DISTANCE) {
					System.out.println("distance less than STOPPING_DISTANCE, drive stopped");
					drive(0, 0, 0, 0);
				} else {
					mecanumDrive(Drive.xaxis, Drive.yaxis, Drive.zaxis, Drive.angle);
				}
			}
			mecanumDrive(Drive.xaxis, Drive.yaxis, Drive.zaxis, Drive.angle);
		}
	}

	/**
	 * Starts the drive thread. Call this after initializing a Drive object and
	 * before any other Drive methods.
	 */
	public void startDrive() {
		synchronized (threadLock) {
			if (threadLock)
				return;
			threadLock = true;
		}
		new Thread(this, "driveThread").start();
	}

	/**
	 * Call THIS method to drive. Must call drive.startDrive() first to
	 * initialize the thread.
	 * 
	 * @param xaxis
	 *            The x-axis of the joystick
	 * @param yaxis
	 *            The y-axis of the joystick
	 * @param zaxis
	 *            The rotational axis of the joystick
	 * @param angle
	 *            The angle read from the gyro. Pass 0 for robot-centric driving
	 * 
	 */
	public static void drive(double xaxis, double yaxis, double zaxis, double angle) {
		Drive.xaxis = xaxis;
		Drive.yaxis = yaxis;
		Drive.zaxis = zaxis;
		Drive.angle = angle;
	}

	public double getHeading() {
		double angle;
		if (headless) {
			angle = ahrs.getAngle() + headingOffset;

			if (angle > 0) {
				angle %= 360;
			} else if (angle < 0) {
				angle = -(Math.abs(angle) % 360) + 360;
			}
		} else {
			angle = 0;
		}
		return angle;
	}

	private void turn(double degrees) {
		double speed = 0.5;
		double heading = getHeading();
		while (heading > (degrees + 1) || heading < (degrees - 1)) {
			if (heading < degrees) {
				speed = -0.5;
				double distance = (360 - degrees) + heading;
				if (distance < (degrees - heading)) {
					speed = 0.5;
				}
				// System.out.println("1: " + heading + " " + speed + " " +
				// distance + " " + degrees);
			} else if (heading > degrees) {
				double distance = degrees + (360 - heading);
				speed = 0.5;
				if (distance < (heading - degrees)) {
					speed = -0.5;
				}
				// System.out.println("2: " + heading + " " + speed + " " +
				// distance + " " + degrees);
			}
			drive(0.0, 0.0, speed, 0);
			heading = getHeading();
		}
		drive(0.0, 0.0, 0.0, 0);

	}

	public void turnAngle(double degrees) {
		double heading = getHeading();
		double desired;
		degrees = degrees % 360;
		desired = heading + degrees;
		if (desired < 0) {
			desired = 360 + desired;
		} else if (desired > 360) {
			desired = desired - 360;
		}
		turn(desired);
	}

	public void turnToAngle(double degrees) {
		double heading = 0;
		double desired;
		degrees = degrees % 360;
		desired = heading + degrees;
		if (desired < 0) {
			desired = 360 + desired;
		} else if (desired > 360) {
			desired = desired - 360;
		}
		turn(desired);
	}

	/**
	 * 
	 * This function makes the robot stay on a straight line when moving
	 * forward/backward or left/right by taking values from the joystick and
	 * when the other two axises are within the deadzone it turns the robot to
	 * the angle that we were previously at. This eliminates drift
	 * (theoretically)
	 * 
	 * @param xaxis
	 *            The X axis value from the joystick
	 * @param yaxis
	 *            The Y axis value from the joystick
	 * @param zaxis
	 *            The Z axis value from the joystick
	 * @param currentAngle
	 *            The angle the robot was previously at in the current iteration
	 */
	public void angleLock(double xaxis, double yaxis, double zaxis, double currentAngle) {
		if (Math.abs(yaxis) > 0 && xaxis == 0 && zaxis == 0) {
			// TODO there is a chance that this should be turnToAngle()
			driveAngle(currentAngle);
		}

		if (Math.abs(xaxis) > 0 && yaxis == 0 && zaxis == 0) {
			driveAngle(currentAngle);
		}
	}

	/**
	 * Same as regular angleLock() but with no parameters to make it easier to
	 * read
	 */
	public void angleLock() {

		if (autoDrive) {
			driveAngle(currentAngle);
		}
	}

	public void switchHeadless() {
		this.headless = !this.headless;
	}

	public boolean getHeadless() {
		return this.headless;
	}

	public void setHeadingOffset(double offset) {
		this.headingOffset = offset;
	}

	public static void setAutoDrive(boolean autoDrive) {
		Drive.autoDrive = autoDrive;
	}

	/**
	 * This will drive the robot in a direction for the specified time and at the specified speed.
	 * 
	 * @param angleDeg
	 *            An angle measurement in degrees.
	 * @param time
	 *            A time measurement in milliseconds.
	 * @param speed
	 * 			  A speed to drive the robot. 1 is full speed, defaults to 0.5
	 */
	public void driveDirection(double angleDeg, int time, double speed) {
		double timer = System.currentTimeMillis();
		drive(0, -speed, 0, -angleDeg);

		while (System.currentTimeMillis() - timer < time) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		drive(0, 0, 0, 0);
	}

}
