package edu.ucsb.ece150.locationplus;

/*
 * This class is provided as a way for you to store information about a single satellite. It can
 * be helpful if you would like to maintain the list of satellites using an ArrayList (i.e.
 * ArrayList<Satellite>). As in Homework 3, you can then use an Adapter to update the list easily.
 *
 * You are not required to implement this if you want to handle satellite information in using
 * another method.
 */
public class Satellite {
    // [TODO] Define private member variables
    private float azimuth;
    private float elevation;
    private float carrier_frequency;
    private float c_no;
    private int constellation;
    private final int SVID;

    // [TODO] Write the constructor
    public Satellite(float azimuth, float elevation, float carrier_frequency, float c_no, int constellation, int SVID) {
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.carrier_frequency = carrier_frequency;
        this.c_no = c_no;
        this.constellation = constellation;
        this.SVID = SVID;
    }

    // [TODO] Implement the toString() method. When the Adapter tries to assign names to items
    // in the ListView, it calls the toString() method of the objects in the ArrayList
    @Override
    public String toString() {
        String text = "";
        text += "Azimuth " + azimuth + " ◦" + "\n";
        text += "Elevation " + elevation + " ◦" + "\n\n";
        text += "Carrier Frequency " + carrier_frequency + " Hz" + "\n";
        text += "C/NO: " + c_no + " dB Hz" + "\n\n";
        text += "Constellation: " + constellation + "\n";
        text += "SVID: " + SVID + "\n";
        return text;
    }
}
