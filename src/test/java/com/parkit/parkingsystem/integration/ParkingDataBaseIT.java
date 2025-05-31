package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static junit.framework.Assert.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static final ParkingSpotDAO parkingSpotDAO = new ParkingSpotDAO();
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
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
    public static void tearDown() {
    }

    @Test
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(ticket);
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());

        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertFalse(parkingSpot.isAvailable());
    }

    @Test
    public void testParkingLotExit() {
        testParkingACar();

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket.getOutTime());
        assertEquals(0.0, ticket.getPrice(), 0.01);

    }

    @Test
    public void testParkingLotExitRecurringUser() {
        testParkingACar();
        Date outTime1 = new Date(System.currentTimeMillis() + (60 * 60 * 1000));

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processExitingVehicle();

        parkingService.processIncomingVehicle();
        Date outTime2 = new Date(System.currentTimeMillis() + (120 * 60 * 1000));
        parkingService.processExitingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        assertTrue(Fare.CAR_RATE_PER_HOUR > ticket.getPrice());

    }
}
