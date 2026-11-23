import dev.skijump.physics.*;
import java.util.Locale;
public class Scen2 { public static void main(String[] a){ HillGeometry h=HillGeometry.forK(125,140);
 for(double e: new double[]{-120,-100,-90,-80,-70,-60,-50,-40,60,80,100,110,120,140}){ JumpSimulation s=PhysicsHarness.run(h,12,1.0,5,0,0,0,e,GearStats.STANDARD,JumperStats.AVERAGE,false);
 System.out.printf(Locale.US,"err %5.0f dist=%6.1f ft=%.2f a0dev=%.1f q=%.2f%n", e, s.distance, s.flightTime, s.meanAlphaDev(), s.takeoffQuality);} } }
