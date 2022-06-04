package com.roombookings.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public void testAvailableRoomsWithMTBookings() throws InterruptedException {
        class RoomBookTask implements Runnable {

            private RoomBookings roomBookings;
            private String name;

            public RoomBookTask(RoomBookings roomBookings, String name) {
                this.roomBookings = roomBookings;
                this.name = name;
            }

            int getRandomNumber(int min, int max) {
                return (int) ((Math.random() * (max - min)) + min);
            }

            @Override
            public void run() {
                for (int i = 1; i < 3; i++) {
                    try {
                        Thread.sleep((long)(Math.random() * 1000));
                        LocalDate date1 = LocalDate.of(2022, 6, i);
                        roomBookings.makeBooking(date1, i, "Smith#" + i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (RoomBookings.RoomBookingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

        for (int i = 1; i < 3; i++) {
            RoomBookTask task = new RoomBookTask(roomBookings, "Task" + i);
            executor.execute(task);
        }
        executor.awaitTermination(2, TimeUnit.SECONDS);

        LocalDate date1 = LocalDate.of(2022, 6, 1);
        Set<Integer> availableRoomsDate1 = roomBookings.getAvailableRooms(date1);
        assertEquals(9, availableRoomsDate1.size());

        LocalDate date2 = LocalDate.of(2022, 6, 2);
        Set<Integer> availableRoomsDate2 = roomBookings.getAvailableRooms(date2);
        assertEquals(9, availableRoomsDate2.size());

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