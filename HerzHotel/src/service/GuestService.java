package service;

import exception.DuplicateRecordException;
import model.Guest;

import java.util.ArrayList;
import java.util.List;

public class GuestService {

    private final ArrayList<Guest> guests;

    public GuestService() {
        guests = new ArrayList<>();
    }

    public void addGuest(Guest guest) throws DuplicateRecordException {
        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null.");
        }

        if (findGuestById(guest.getGuestId()) != null) {
            throw new DuplicateRecordException(
                "A guest with ID " + guest.getGuestId() + " already exists."
            );
        }

        guests.add(guest);
    }

    public void updateGuest(String guestId, Guest updatedGuest)
            throws DuplicateRecordException {

        Guest existingGuest = findGuestById(guestId);

        if (existingGuest == null) {
            throw new IllegalArgumentException("Guest not found.");
        }

        if (updatedGuest == null) {
            throw new IllegalArgumentException("Updated guest cannot be null.");
        }

        String newId = updatedGuest.getGuestId();

        if (!guestId.equals(newId) && findGuestById(newId) != null) {
            throw new DuplicateRecordException(
                "A guest with ID " + newId + " already exists."
            );
        }

        existingGuest.setGuestId(newId);
        existingGuest.setFullName(updatedGuest.getFullName());
        existingGuest.setEmail(updatedGuest.getEmail());
        existingGuest.setPhoneNumber(updatedGuest.getPhoneNumber());
        existingGuest.setAddress(updatedGuest.getAddress());
    }

    public void deleteGuest(String guestId) {
        Guest guest = findGuestById(guestId);

        if (guest == null) {
            throw new IllegalArgumentException("Guest not found.");
        }

        guests.remove(guest);
    }

    public Guest findGuestById(String guestId) {
        for (Guest guest : guests) {
            if (guest.getGuestId().equalsIgnoreCase(guestId)) {
                return guest;
            }
        }

        return null;
    }

    public List<Guest> searchGuests(String keyword) {
        ArrayList<Guest> results = new ArrayList<>();

        String searchTerm = keyword == null
                ? ""
                : keyword.trim().toLowerCase();

        for (Guest guest : guests) {
            if (guest.getGuestId().toLowerCase().contains(searchTerm)
                    || guest.getFullName().toLowerCase().contains(searchTerm)
                    || guest.getEmail().toLowerCase().contains(searchTerm)
                    || guest.getPhoneNumber().toLowerCase().contains(searchTerm)) {

                results.add(guest);
            }
        }

        return results;
    }

    public List<Guest> getAllGuests() {
        return new ArrayList<>(guests);
    }

    public int getGuestCount() {
        return guests.size();
    }
}