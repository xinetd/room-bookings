package com.roombookings.app;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RoomBookings {

    private int maxRooms;
    private ConcurrentHashMap<LocalDate, Set<Booking>> roomBookingsByDate;
    private ConcurrentHashMap<String, Set<LocalDate>> roomBookingsByGuest;
    private ReentrantLock lock;

    RoomBookings(int maxRooms) {
        this.maxRooms = maxRooms;
        roomBookingsByDate = new ConcurrentHashMap<>();
        roomBookingsByGuest = new ConcurrentHashMap<>();
        lock = new ReentrantLock();
    }

    private <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
        return iterable == null ? Collections.<T>emptyList() : iterable;
    }

    private Set<Integer> setOfMaxRooms() {
        return new HashSet<>(IntStream.rangeClosed(1, maxRooms).boxed().toList());
    }

    private Set<Integer> setOfBookedRooms(LocalDate date) {
        Set<Integer> rooms = new HashSet<Integer>();
        Set<Booking> bookings = roomBookingsByDate.get(date);
        for (Booking booking : emptyIfNull(bookings)) {
            rooms.add(booking.roomNumber);
        }
        return rooms;
    }

    public record Booking (
        LocalDate date,
        Integer roomNumber,
        String guestName

    ) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Booking)) return false;

            Booking booking = (Booking) o;

            return date.equals(booking.date) &&
                    roomNumber.equals(booking.roomNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, roomNumber);
        }
    }

    public class RoomBookingException
            extends RuntimeException {
        public RoomBookingException(String errorMessage, Throwable err) {
            super(errorMessage, err);
        }
    }

    public void makeBooking(LocalDate date, Integer roomNumber, String guestName) throws RoomBookingException, InterruptedException {

        boolean isLockAcquired = lock.tryLock(1, TimeUnit.SECONDS);

        if(isLockAcquired) {
            try {
                Booking booking = new Booking(date, roomNumber, guestName);

                if(roomBookingsByDate.containsKey(date)) {
                   if (!roomBookingsByDate.get(date).add(booking))
                       throw new RoomBookingException("Room booking error", null);
                }
                else {
                    HashSet<Booking> roomsByDate = new HashSet<>();
                    roomsByDate.add(booking);
                    roomBookingsByDate.put(date, roomsByDate);
                }

                if(roomBookingsByGuest.containsKey(guestName)) {
                    roomBookingsByGuest.get(guestName).add(date);
                }
                else {
                    HashSet<LocalDate> roomsByGuest = new HashSet<>();
                    roomsByGuest.add(date);
                    roomBookingsByGuest.put(guestName, roomsByGuest);
                }
            } finally {
                lock.unlock();
            }
        }

    }

    public Set<Integer> getAvailableRooms(LocalDate date) {
        Set<Integer> allRooms = setOfMaxRooms();
        Set<Integer> bookedRooms = setOfBookedRooms(date);

        Set<Integer> availableRooms = allRooms.stream()
            .filter( x -> ! bookedRooms.contains(x) )
            .collect(Collectors.toSet());

        return availableRooms;
    }

    public Set<Booking> getGuestBookings(String guestName) {
        Set<Booking> bookings = new HashSet<>();

        Set<LocalDate> dates = roomBookingsByGuest.get(guestName);
        for(LocalDate date : dates) {
            Set<Booking> bookingsByDate = roomBookingsByDate.get(date).stream()
                .filter( x -> x.guestName == guestName )
                .collect(Collectors.toSet());
            bookings.addAll(bookingsByDate);
        }

        return bookings;
    }
}
