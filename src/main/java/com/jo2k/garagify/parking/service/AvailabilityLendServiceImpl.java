package com.jo2k.garagify.parking.service;

import com.jo2k.dto.TimeRangeDto;
import com.jo2k.garagify.lendoffer.persistence.model.LendOffer;
import com.jo2k.garagify.lendoffer.persistence.repository.LendOfferRepository;
import com.jo2k.garagify.parking.api.ParkingAvailability;
import com.jo2k.garagify.parking.api.ParkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service("availabilityLendService")
@RequiredArgsConstructor
public class AvailabilityLendServiceImpl implements ParkingAvailability {
    private final LendOfferRepository lendOfferRepository;
    private final ParkingService parkingService;

    @Override
    public List<TimeRangeDto> getTimeRanges(Integer parkingId, OffsetDateTime untilWhen) {
        OffsetDateTime now = OffsetDateTime.now();
        var spots = parkingService.getParkingSpotsByParkingId(parkingId);

        List<TimeRangeDto> allFree = new ArrayList<>();

        for (var spot : spots) {
            UUID spotUuid = spot.getSpotUuid();
            List<LendOffer> offers = getSortedLendOffers(spotUuid);
            allFree.addAll(getFreeTimeRangesForSpot(now, untilWhen, offers));
        }
        return mergeTimeRanges(allFree);
    }

    @Override
    public List<UUID> getSpots(Integer parkingId, OffsetDateTime from, OffsetDateTime until) {
        var spots = parkingService.getParkingSpotsByParkingId(parkingId);
        List<UUID> result = new ArrayList<>();

        for (var spot : spots) {
            UUID spotUuid = spot.getSpotUuid();
            if (isSpotFreeForLend(spotUuid, from, until)) {
                result.add(spotUuid);
            }
        }
        return result;
    }

    private List<LendOffer> getSortedLendOffers(UUID spotUuid) {
        List<LendOffer> offers = lendOfferRepository.findByParkingSpotId(spotUuid);
        offers.sort(Comparator.comparing(LendOffer::getStartDate));
        return offers;
    }

    private List<TimeRangeDto> getFreeTimeRangesForSpot(
            OffsetDateTime from, OffsetDateTime untilWhen, List<LendOffer> offers) {

        List<TimeRangeDto> freeSlots = new ArrayList<>();
        OffsetDateTime windowStart = from;

        for (LendOffer offer : offers) {
            if (windowStart.isBefore(offer.getStartDate())) {
                freeSlots.add(new TimeRangeDto(windowStart, offer.getStartDate()));
            }
            windowStart = offer.getEndDate().isAfter(windowStart) ? offer.getEndDate() : windowStart;
        }
        if (windowStart.isBefore(untilWhen)) {
            freeSlots.add(new TimeRangeDto(windowStart, untilWhen));
        }
        return freeSlots;
    }

    private boolean isSpotFreeForLend(UUID spotUuid, OffsetDateTime from, OffsetDateTime until) {
        return lendOfferRepository.findByParkingSpotId(spotUuid)
                .stream()
                .noneMatch(lo -> from.isBefore(lo.getEndDate()) && until.isAfter(lo.getStartDate()));
    }

    private List<TimeRangeDto> mergeTimeRanges(List<TimeRangeDto> ranges) {
        if (ranges.isEmpty()) return Collections.emptyList();

        ranges.sort(Comparator.comparing(TimeRangeDto::getStart));

        List<TimeRangeDto> merged = new ArrayList<>();
        TimeRangeDto prev = ranges.get(0);

        for (int i = 1; i < ranges.size(); i++) {
            TimeRangeDto curr = ranges.get(i);
            if (!prev.getEnd().isBefore(curr.getStart())) {
                prev = new TimeRangeDto(
                        prev.getStart(),
                        prev.getEnd().isAfter(curr.getEnd()) ? prev.getEnd() : curr.getEnd()
                );
            } else {
                merged.add(prev);
                prev = curr;
            }
        }
        merged.add(prev);
        return merged;
    }
}
