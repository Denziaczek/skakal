package dev.skijump.physics;

/** Wynik pojedynczego skoku (punktacja zgodna z uproszczonymi zasadami FIS). */
public final class JumpResult {
    public double distance;
    public double distancePoints;
    public double[] judgeMarks = new double[5];
    public double styleTotal;
    public double windPoints;
    public double gatePoints;
    public double total;
    public double avgHead;
    public double takeoffSpeed;
    public double takeoffErrMs;
    public boolean perfectTakeoff;
    public double telemarkQuality;
    public double impactSpeed;
    public boolean fell;
    public String fallReason = "";
    public int gate;

    public JumpResult copy() {
        JumpResult r = new JumpResult();
        r.distance = distance; r.distancePoints = distancePoints; r.judgeMarks = judgeMarks.clone();
        r.styleTotal = styleTotal; r.windPoints = windPoints; r.gatePoints = gatePoints; r.total = total;
        r.avgHead = avgHead; r.takeoffSpeed = takeoffSpeed; r.takeoffErrMs = takeoffErrMs;
        r.perfectTakeoff = perfectTakeoff; r.telemarkQuality = telemarkQuality; r.impactSpeed = impactSpeed;
        r.fell = fell; r.fallReason = fallReason; r.gate = gate;
        return r;
    }
}
