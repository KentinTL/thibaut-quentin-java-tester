package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private final static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private final FareCalculatorService fareCalculatorService = new FareCalculatorService();

    @Mock(lenient = true)
    private static InputReaderUtil inputReaderUtil;
    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown(){

    }

    @Test
    @DisplayName("Test ticket is created and saved in database, test parking table availability is updated")
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(fareCalculatorService, inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "The ticket is actually saved in database");
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertFalse(ticket.getParkingSpot().isAvailable());
        assertEquals(0, ticket.getPrice());
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());
    }

    @Test
    public void testParkingLotExit(){
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticketDAO.saveTicket(ticket);
        ParkingService parkingService = new ParkingService(fareCalculatorService, inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();
        Ticket ticketFound = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticketFound);
        assertNotNull(ticketFound.getOutTime());
        assertEquals("ABCDEF", ticketFound.getVehicleRegNumber());
        assertTrue(ticketFound.getParkingSpot().isAvailable());
        assertEquals(1.5, ticketFound.getPrice());
        assertNotNull(ticketFound.getInTime());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(this.fareCalculatorService, this.inputReaderUtil, this.parkingSpotDAO, this.ticketDAO);
        //First fake ticket created
        Ticket firstTicket = new Ticket();
        firstTicket.setInTime(new Date(System.currentTimeMillis() - (1000*60*60)));
        firstTicket.setVehicleRegNumber("ABCDEF");
        firstTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        firstTicket.setOutTime(new Date(System.currentTimeMillis() - (1000*60*120)));
        firstTicket.setPrice(1.5);
        ticketDAO.saveTicket(firstTicket);
        //Second fake ticket created
        Ticket secondTicket = new Ticket();
        secondTicket.setInTime(new Date(System.currentTimeMillis() + (1000*60*60)));
        secondTicket.setVehicleRegNumber("ABCDEF");
        secondTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        secondTicket.setOutTime(new Date(System.currentTimeMillis() + (1000*60*120)));
        secondTicket.setPrice(1.5);
        ticketDAO.saveTicket(secondTicket);
        //Execute necessary scripts
        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ticket.setOutTime(new Date(System.currentTimeMillis() + (60*60*1000)));//Update out time + 1 hour to calculate a price
        assertNotNull(ticket);
        assertNotNull(ticket.getInTime());
        assertNotNull(ticket.getOutTime());
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertEquals(1.43, ticket.getPrice());
    }

}
