import dev.skijump.physics.*;
public class Trace2 { public static void main(String[] a){ HillGeometry h=HillGeometry.forK(125,140);
 System.out.println("head +3"); PhysicsHarness.run(h,12,1.0,5,3,0,0,null,GearStats.STANDARD,JumperStats.AVERAGE,true);
 System.out.println("head 0"); PhysicsHarness.run(h,12,1.0,5,0,0,0,null,GearStats.STANDARD,JumperStats.AVERAGE,true);} }
