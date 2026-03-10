package de.ceddyie.organizerbackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "http://localhost:5173/")
public class EventController {
}
