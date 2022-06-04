package com.roombookings.app;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RoomBookings {

    private int maxRooms;
    private ConcurrentHashMap<LocalDate, Set<Booking>> roomBookingsByDate;
    private ConcurrentHashMap<String, Set<LocalDate>> roomBookingsByGuest;

    RoomBookings(int maxRooms) {
        this.maxRooms = maxRooms;
        roomBookingsByDate = new ConcurrentHashMap<>();
        roomBookingsByGuest = new ConcurrentHashMap<>();
    }

    private <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
        return iterable == null ? Collections.<T>emptyList() : iterable;
    }

    private Set<Integer> setOfMaxRooms() {
        return new HashSet<>(IntStream.rangeClosed(1, maxRooms).boxed().toList());
    }

    private Set<Integer> setOfBookedRooms(LocalDate date) {
        Set<Integer> rooms = ConcurrentHashMap.newKeySet();

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

    public void makeBooking(LocalDate date, Integer roomNumber, String guestName) throws RoomBookingException {

        Booking booking = new Booking(date, roomNumber, guestName);

        roomBookingsByDate.compute(date, (key, value) -> {
            if(value == null) {
                Set<Booking> roomsByDate = ConcurrentHashMap.newKeySet();
                roomsByDate.add(booking);
                return roomsByDate;
            }
            else {
                if (!value.add(booking)) {
                    throw new RoomBookingException("Room booking error", null);
                }
                return value;
            }
        });

       roomBookingsByGuest.compute(guestName, (key, value) -> {
            if(value == null) {
                Set<LocalDate> roomsByGuest = ConcurrentHashMap.newKeySet();
                roomsByGuest.add(date);
                return roomsByGuest;
            }
            else {
                value.add(date);
                return value;
            }
        });
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
        Set<Booking> bookings = ConcurrentHashMap.newKeySet();

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
