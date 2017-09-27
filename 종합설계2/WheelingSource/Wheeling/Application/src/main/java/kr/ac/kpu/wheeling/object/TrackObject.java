package kr.ac.kpu.wheeling.object;

/**
 * Created by limhj_000 on 2017-06-20.
 */

public class TrackObject {
    private int time;
    private double lat;
    private double lon;
    private double alt;
    private double speed, maxspeed,avrspeed;
    private String address;
    private Long mtime;
    private int bid;

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public Long getMtime() {
        return mtime;
    }

    public void setMtime(Long mtime) {
        this.mtime = mtime;
    }

    public double getAvrspeed() {
        return avrspeed;
    }

    public void setAvrspeed(double avrspeed) {
        this.avrspeed = avrspeed;
    }

    private double distance;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }


    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getMaxspeed() {
        return maxspeed;
    }

    public void setMaxspeed(double maxspeed) {
        this.maxspeed = maxspeed;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getAlt() {
        return alt;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }

    @Override
    public String toString() {
        return "TrackObject{" +
                " bid =" + bid +
                ", time =" + time +
                ", lat= " + lat +
                ", lon=" + lon +
                ", alt=" + alt +
                ", distance=" + distance +
                ", speed=" + speed +
                ", avrspeed=" + avrspeed +
                ", maxspeed=" + maxspeed +
                ", address=" + address +
                ", mtime=" + mtime +
                '}';
    }
}
