package dev.skijump.entity;

/** Źródło pozy skoczka dla renderera (zsynchronizowane z serwera). Fazy: 0 belka, 1 rozbieg, 2 lot, 3 lądowanie, 4 brak. */
public interface PoseSource {
    int posePhase();

    float posePitch();

    float poseRoll();

    float poseV();

    float poseTumble();

    boolean poseTelemark();

    boolean poseCrouch();
}
