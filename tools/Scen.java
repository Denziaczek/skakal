import dev.skijump.physics.*;
import java.util.Locale;
public class Scen {
    static void line(String n, JumpSimulation s){
        System.out.printf(Locale.US,"%-26s dist=%6.1f fell=%-5b %-12s imp=%4.1f tele=%.2f bal=%.2f ft=%.2f vedge=%.1f roll=%.1f dev=%.1f z=%.1f%n", n, s.distance, s.fell, s.fallReason, s.impactSpeed, s.telemarkQuality, s.balanceQuality, s.flightTime, s.takeoffSpeed, s.meanAbsRoll(), s.meanAlphaDev(), s.landingZ);
    }
    public static void main(String[] a){
        HillGeometry h=HillGeometry.forK(125,140);
        GearStats best=new GearStats(0.8,0.7,0.6,0.6,0.5,0.55,0.5);
        JumperStats good=new JumperStats(0.9,0.9,0.9), bad=new JumperStats(0.2,0.2,0.2);
        System.out.println("-- timing (skill1, avg)");
        for(double e: new double[]{-150,-80,-40,-15,0,15,40,80,150,300}) line("err "+e, PhysicsHarness.run(h,12,1.0,5,0,0,0,e,GearStats.STANDARD,JumperStats.AVERAGE,false));
        System.out.println("-- wind head (gust 0)");
        for(double w: new double[]{-4,-2,0,2,4}) line("head "+w, PhysicsHarness.run(h,12,1.0,5,w,0,0,0.0,GearStats.STANDARD,JumperStats.AVERAGE,false));
        System.out.println("-- wind side + gusts");
        line("side 3", PhysicsHarness.run(h,12,1.0,5,0,3,0,0.0,GearStats.STANDARD,JumperStats.AVERAGE,false));
        for(long sd=1; sd<=4; sd++) line("gust 3 seed "+sd, PhysicsHarness.run(h,12,1.0,sd,1,1,3,0.0,GearStats.STANDARD,JumperStats.AVERAGE,false));
        System.out.println("-- skill");
        for(double sk: new double[]{0.1,0.3,0.5,0.7,0.9}) for(long sd=1; sd<=2; sd++) line("skill "+sk+" s"+sd, PhysicsHarness.run(h,12,sk,sd,1,0,1.5,null,GearStats.STANDARD,JumperStats.AVERAGE,false));
        System.out.println("-- gear/stats");
        line("no gear avg", PhysicsHarness.run(h,12,1.0,5,0,0,0,0.0,GearStats.NONE,JumperStats.AVERAGE,false));
        line("best gear avg", PhysicsHarness.run(h,12,1.0,5,0,0,0,0.0,best,JumperStats.AVERAGE,false));
        line("best gear good", PhysicsHarness.run(h,12,1.0,5,0,0,0,0.0,best,good,false));
        line("none bad", PhysicsHarness.run(h,12,1.0,5,0,0,0,0.0,GearStats.NONE,bad,false));
        System.out.println("-- gust 5 no skill");
        for(long sd=1; sd<=3; sd++) line("storm unskilled s"+sd, PhysicsHarness.run(h,12,0.2,sd,3,3,5,null,GearStats.STANDARD,bad,false));
    }
}
