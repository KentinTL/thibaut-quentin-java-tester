package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class FareCalculatorService {
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        double inMinutes = ticket.getInTime().getTime();
        double outMinutes = ticket.getOutTime().getTime();

        double duration = (outMinutes - inMinutes)/(1000*60*60);

        //Looking for if time is 30 minutes or less
        if(duration <=0.5) {
            ticket.setPrice(0);
        } else {
            double priceDiscount;

            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    priceDiscount = discount ? Fare.CAR_RATE_PER_HOUR * 0.95 : Fare.CAR_RATE_PER_HOUR;
                    break;
                }
                case BIKE: {
                    priceDiscount = discount ? Fare.BIKE_RATE_PER_HOUR * 0.95 : Fare.BIKE_RATE_PER_HOUR;
                    break;
                }
                default: throw new IllegalArgumentException("Unkown Parking Type");
            }

            double ticketPrice = duration * priceDiscount;
            ticketPrice = roundToTwoNumber(ticketPrice);

            ticket.setPrice(ticketPrice);
        }
    }
    public double roundToTwoNumber (double doubleNumber){
        BigDecimal bd = new BigDecimal(doubleNumber);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}