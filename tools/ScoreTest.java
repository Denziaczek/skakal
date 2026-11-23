import dev.skijump.physics.*;
import java.util.Arrays;
public class ScoreTest { public static void main(String[] a){ HillGeometry h=HillGeometry.forK(125,140);
 for (double sk: new double[]{0.2,0.6,1.0}) { JumpSimulation s=PhysicsHarness.run(h,12,sk,7,0.5,0.5,1.5,null,GearStats.STANDARD,JumperStats.AVERAGE,false);
  JumpResult r=Scoring.evaluate(s,12,7);
  System.out.printf("skill %.1f dist=%.1f dp=%.1f marks=%s style=%.1f wind=%.1f gate=%.1f total=%.1f fell=%b%n",sk,r.distance,r.distancePoints,Arrays.toString(r.judgeMarks),r.styleTotal,r.windPoints,r.gatePoints,r.total,r.fell);} } }
