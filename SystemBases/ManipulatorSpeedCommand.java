package frc.robot.SyncedLibraries.SystemBases;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.ArrayList;

import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;

public class ManipulatorSpeedCommand extends Command {
  private ManipulatorBase manipulator;
  private double targetSpeed;
  private double tolerance;
  private boolean endOnTarget = false;
  private PIDController[] pid;
  private CANSparkMax[] motors;
  private RelativeEncoder[] encoders;
  public int onTargetCounterStart = 10;
  private int onTargetCounter = onTargetCounterStart;
  public boolean atSpeed = false;
  
  public ManipulatorSpeedCommand(ManipulatorBase manipulator, double speed, double tolerance, double kP, double kI,
  double kD) {
    this.manipulator = manipulator;
    this.targetSpeed = speed;
    this.tolerance = tolerance;
    motors = manipulator.getMotors();
    encoders = manipulator.getEncoders();
    ArrayList<PIDController> pidList = new ArrayList<PIDController>();
    for (int i = 0; i < motors.length; i++) {
      pidList.add(new PIDController(kP, kI, kD));
      pidList.get(pidList.size() - 1).setTolerance(tolerance);
    }
    pid = pidList.toArray(new PIDController[0]);

    addRequirements(manipulator);
  }

  @Override
  public void initialize() {
    manipulator.stop();
    for (int i = 0; i < motors.length; i++) {
      pid[i].setTolerance(tolerance);
      pid[i].setSetpoint(targetSpeed);
    }

    if (targetSpeed == Integer.MAX_VALUE) {
      DriverStation.reportError("ManipulatorSpeedCommand: speed not set", true);
      CommandScheduler.getInstance().cancel(this);
    }
  }

  @Override
  public void execute() {
    for (int i = 0; i < motors.length; i++) {
      double pidSpeed = pid[i].calculate(encoders[i].getVelocity(), targetSpeed);
      motors[i].set(pidSpeed);
    }
    atSpeed = isAtSpeed();
  }

  @Override
  public void end(boolean interrupted) {
    manipulator.stop();
  }

  @Override
  public boolean isFinished() {
    return endOnTarget && atSpeed;
  }

  public void setTargetSpeed(double speed) {
    this.targetSpeed = speed;
    // initialize();
    for (int i = 0; i < pid.length; i++) {
      pid[i].setSetpoint(speed);
    }
  }

  public double getTargetSpeed() {
    return targetSpeed;
  }

  public double getTolerance() {
    return tolerance;
  }

  private boolean isAtSpeed() {
    if (currentlyAtSpeed()) {
      onTargetCounter--;
    } else {
      onTargetCounter = onTargetCounterStart;
    }
    return onTargetCounter <= 0;
  }

  private boolean currentlyAtSpeed() {
    for (int i = 0; i < motors.length; i++) {
      if (encoders[i].getVelocity() - targetSpeed > tolerance * 1.5) {
        return false;
      }
    }
    return Math.abs(manipulator.getCurrentSpeed() - targetSpeed) < tolerance * 1.5;
  }

  public void setEndOnTarget(boolean endOnTarget) {
    this.endOnTarget = endOnTarget;
  }
}
