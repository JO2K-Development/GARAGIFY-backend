package com.jo2k.garagify.parking.service;

import com.jo2k.dto.BorrowDTO;
import com.jo2k.dto.TimeRangeRequest;
import com.jo2k.garagify.common.exception.InvalidBorrowException;
import com.jo2k.garagify.common.exception.ObjectNotFoundException;
import com.jo2k.garagify.lendoffer.persistence.model.LendOffer;
import com.jo2k.garagify.lendoffer.persistence.repository.LendOfferRepository;
import com.jo2k.garagify.parking.api.ParkingActionService;
import com.jo2k.garagify.parking.mapper.ParkingBorrowMapper;
import com.jo2k.garagify.parking.persistence.model.ParkingBorrow;
import com.jo2k.garagify.parking.persistence.model.ParkingLend;
import com.jo2k.garagify.parking.persistence.model.ParkingSpot;
import com.jo2k.garagify.parking.persistence.repository.ParkingBorrowRepository;
import com.jo2k.garagify.parking.persistence.repository.ParkingLendRepository;
import com.jo2k.garagify.parking.persistence.repository.ParkingSpotRepository;
import com.jo2k.garagify.user.model.User;
import com.jo2k.garagify.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("parkingBorrowService")
@RequiredArgsConstructor
public class ParkingBorrowServiceImpl implements ParkingActionService<BorrowDTO> {
    private final ParkingBorrowRepository parkingBorrowRepository;
    private final UserService userService;
    private final ParkingBorrowMapper parkingBorrowMapper;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingLendRepository parkingLendRepository;

    @Override
    public BorrowDTO create(Integer parkingId, UUID spotUuid, TimeRangeRequest timeRange) {
        User currentUser = userService.getCurrentUser();

        ParkingSpot spot = parkingSpotRepository.findByParking_IdAndSpotUuid(parkingId, spotUuid)
                .orElseThrow(() -> new InvalidBorrowException("Parking spot not found"));

        UUID ownerId = spot.getOwner() != null ? spot.getOwner().getId() : null;
        if (ownerId != null && spot.getOwner().getId().equals(currentUser.getId())) {
            throw new InvalidBorrowException("You cannot borrow your own parking spot");
        }

        boolean overlap = parkingBorrowRepository.existsOverlap(
                parkingId, spotUuid, timeRange.getFromWhen(), timeRange.getUntilWhen()
        );
        if (overlap) {
            throw new InvalidBorrowException("Borrow exists in this time range for this spot");
        }

        ParkingLend lendOffer = parkingLendRepository
                .findFirstByParkingSpot_SpotUuidAndParkingSpot_Parking_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        spotUuid,
                        parkingId,
                        timeRange.getFromWhen(),
                        timeRange.getUntilWhen()
                )
                .orElseThrow(() -> new InvalidBorrowException("No valid lend offer found for this time range"));

        ParkingBorrow borrow = ParkingBorrow.builder()
                .borrowTime(timeRange.getFromWhen())
                .returnTime(timeRange.getUntilWhen())
                .user(currentUser)
                .parkingSpot(spot)
                .parkingLendOffer(lendOffer)
                .build();

        parkingBorrowRepository.save(borrow);
        return parkingBorrowMapper.toDTO(borrow);
    }

    @Override
    public Page<BorrowDTO> getForCurrentUser(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return parkingBorrowRepository.findAllByUser(currentUser, pageable)
                .map(parkingBorrowMapper::toDTO);
    }

    @Transactional
    public void delete(UUID id) {
        if (!parkingBorrowRepository.existsById(id)) {
            throw new ObjectNotFoundException("Borrow with id " + id + " not found");
        }
        parkingBorrowRepository.deleteById(id);
    }
}
