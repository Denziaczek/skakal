import dev.skijump.physics.*;
public class Trace { public static void main(String[] a){ HillGeometry h=HillGeometry.forK(125,140); PhysicsHarness.run(h,12,1.0,5,0,0,0,0.0,GearStats.STANDARD,JumperStats.AVERAGE,true);} }
