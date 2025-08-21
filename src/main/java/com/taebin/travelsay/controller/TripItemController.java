package com.taebin.travelsay.controller;


import com.taebin.travelsay.domain.trip.TripItem;
import com.taebin.travelsay.dto.trip.item.request.CreateItemRequest;
import com.taebin.travelsay.dto.trip.item.request.MoveItemRequest;
import com.taebin.travelsay.dto.trip.item.request.ReorderItemRequest;
import com.taebin.travelsay.dto.trip.item.request.UpdateItemRequest;
import com.taebin.travelsay.dto.trip.item.response.ItemResponse;
import com.taebin.travelsay.security.MemberDetails;
import com.taebin.travelsay.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripItemController {

    private final TripService tripService;

    private String mid(MemberDetails memberDetails) {
        if (memberDetails == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        return memberDetails.getMemberId();
    }

    @GetMapping("/days/{dayId}/items")
    public ResponseEntity<List<ItemResponse>> listItems(@PathVariable Long dayId,
                                                        @AuthenticationPrincipal MemberDetails principal) {
        List<TripItem> items = tripService.listItems(dayId, mid(principal));
        return ResponseEntity.ok(items.stream()
                .map(i -> new ItemResponse(
                        i.getId(), i.getTitle(), i.getStartTime(), i.getAmount(),
                        i.getMerchant(), i.getMemo(), i.getOrderNo()
                ))
                .toList());
    }

    @PostMapping("/days/{dayId}/items")
    public ResponseEntity<ItemResponse> addItem(@PathVariable Long dayId,
                                                @Valid @RequestBody CreateItemRequest request,
                                                @AuthenticationPrincipal MemberDetails principal) {
        TripItem i = tripService.addItem(dayId, request, mid(principal));
        return ResponseEntity
                .created(URI.create("/api/trips/items/" + i.getId()))
                .body(new ItemResponse(i.getId(), i.getTitle(), i.getStartTime(),
                        i.getAmount(), i.getMerchant(), i.getMemo(), i.getOrderNo()));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long itemId,
                                                   @Valid @RequestBody UpdateItemRequest request,
                                                   @AuthenticationPrincipal MemberDetails principal) {
        TripItem i = tripService.updateItem(itemId, request, mid(principal));
        return ResponseEntity.ok(new ItemResponse(i.getId(), i.getTitle(), i.getStartTime(),
                i.getAmount(), i.getMerchant(), i.getMemo(), i.getOrderNo()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId,
                                           @AuthenticationPrincipal MemberDetails principal) {
        tripService.deleteItem(itemId, mid(principal));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{itemId}/order")
    public ResponseEntity<Void> reorder(@PathVariable Long itemId,
                                        @Valid @RequestBody ReorderItemRequest request,
                                        @AuthenticationPrincipal MemberDetails principal){
        tripService.reorder(itemId, request.newOrderNo(), mid(principal));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{itemId}/move")
    public ResponseEntity<Void> move(@PathVariable Long itemId,
                                     @Valid @RequestBody MoveItemRequest request,
                                     @AuthenticationPrincipal MemberDetails principal) {
        tripService.moveItem(itemId, request.targetDayId(), request.newOrderNo(), mid(principal));
        return ResponseEntity.noContent().build();
    }

}
