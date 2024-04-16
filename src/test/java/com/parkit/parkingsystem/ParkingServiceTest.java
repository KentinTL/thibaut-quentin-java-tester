package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @Mock
    private FareCalculatorService fareCalculatorService;

 @BeforeEach
    public void setUpPerTest() {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            Ticket ticket = getTicket();
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);

            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

            parkingService = new ParkingService(fareCalculatorService, inputReaderUtil, parkingSpotDAO, ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }
    @DisplayName("Add new Class Ticket to use getTicket()")
    private static Ticket getTicket() {
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        return ticket;
    }

    @Test
    @DisplayName("Testing Vehicle out who has a discount")
    public void processExitingVehicleTest() throws Exception {
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(1);
        doNothing().when(fareCalculatorService).calculateFare(any(Ticket.class));
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processExitingVehicle();
        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, Mockito.times(1)).getTicket("ABCDEF");
        verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF");
        verify(fareCalculatorService, Mockito.times(1)).calculateFare(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    @DisplayName("Testing Incoming vehicle with everything ok")
    public void testProcessIncomingVehicle() throws Exception {
        Mockito.reset(ticketDAO);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(0);
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingService.getNextParkingNumberIfAvailable()).thenAnswer(invocationOnMock -> 1);
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

        parkingService.processIncomingVehicle();

        verify(inputReaderUtil, Mockito.times(1)).readVehicleRegistrationNumber();
        verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF"); // Vérifiez si nécessaire
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class)); // Vérifiez si nécessaire
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class)); // Vérifiez si nécessaire
    }
}
