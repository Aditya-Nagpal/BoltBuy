package com.boltbuy.workerservice.controller;

import com.boltbuy.workerservice.model.Order;
import com.boltbuy.workerservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/worker")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class WorkerController {

    private final OrderRepository orderRepository;

    @GetMapping("/recent-orders")
    public List<Order> getRecentOrders() {
        // Fetch last 10 orders sorted by creation time
        return orderRepository.findAll(
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }
}