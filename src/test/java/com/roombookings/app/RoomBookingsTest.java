package com.roombookings.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RoomBookingsTest {
    protected RoomBookings roomBookings;

    @BeforeEach
    protected void setUp(){
        roomBookings = new RoomBookings(10);
    }

    @Test
    public void testAvailableRoomsWithSingleBooking() {
        LocalDate date1 = LocalDate.of(2022, 6, 1);
        roomBookings.makeBooking(date1, 1, "Smith");

        Set<Integer> availableRooms = roomBookings.getAvailableRooms(date1);
        assertEquals(9, availableRooms.size());
    }

    @Test
    public void testAvailableRoomsWithManyBookings() {
        LocalDate date1 = LocalDate.of(2022, 6, 1);
        roomBookings.makeBooking(date1, 1, "Smith");

        LocalDate date2 = LocalDate.of(2022, 6, 2);
        roomBookings.makeBooking(date2, 2, "Smithson");
        roomBookings.makeBooking(date2, 3, "Smithson");

        Set<Integer> availableRooms = roomBookings.getAvailableRooms(date2);
        assertEquals(8, availableRooms.size());
    }

    @Test
    public void testAvailableRoomsWithMTGivenDistinctDay() throws InterruptedException {
        IntStream.range(0,10).parallel().forEach(x -> {
            LocalDate date = LocalDate.of(2022, 6, x+1);
            roomBookings.makeBooking(date, x, "Smith" + x);
        });

        IntStream.range(0,10).parallel().forEach(x -> {
            LocalDate date = LocalDate.of(2022, 6, x+1);
            Set<Integer> availableRooms = roomBookings.getAvailableRooms(date);
            assertEquals(9, availableRooms.size());
        });

    }

    @Test
    public void testAvailableRoomsWithMTGivenAllBookedSameDay() throws InterruptedException {
        LocalDate date = LocalDate.of(2022, 6, 1);

        IntStream.range(0,10).parallel().forEach(x -> {
            roomBookings.makeBooking(date, x, "Smith" + x);
        });

        Set<Integer> availableRooms = roomBookings.getAvailableRooms(date);
        assertEquals(0, availableRooms.size());

    }

    @Test
    public void testGuestBookings() {
        LocalDate date1 = LocalDate.of(2022, 6, 1);
        String guestName = "Smithson";
        roomBookings.makeBooking(date1, 1, guestName);

        Set<RoomBookings.Booking> guestBookings = roomBookings.getGuestBookings(guestName);
        assertEquals(1, guestBookings.size());
    }

    @Test
    public void testBookingError() {
        Throwable exception = assertThrows(RoomBookings.RoomBookingException.class, () -> {
            LocalDate date1 = LocalDate.of(2022, 6, 1);
            roomBookings.makeBooking(date1, 1, "Smith");
            roomBookings.makeBooking(date1, 1, "Smithson");
        });
        assertEquals("Room booking error", exception.getMessage());
    }

}