package com.billpoint.backend.controller;

import com.billpoint.backend.dto.MessageResponse;
import com.billpoint.backend.model.*;
import com.billpoint.backend.repository.*;
import com.billpoint.backend.security.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/shop-owner")
@PreAuthorize("hasRole('SHOP_OWNER')")
public class ShopOwnerController {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private PasswordEncoder encoder;

    private Shop getAuthShop(Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return shopRepository.findByOwner_Id(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: Shop not found for this owner"));
    }

    @GetMapping("/profile")
    public ResponseEntity<Shop> getShopProfile(Authentication authentication) {
        Shop shop = getAuthShop(authentication);
        return ResponseEntity.ok(shop);
    }

    @GetMapping("/attendance-code")
    public ResponseEntity<MessageResponse> getAttendanceCode(Authentication authentication) {
        Shop shop = getAuthShop(authentication);
        return ResponseEntity.ok(new MessageResponse(shop.getAttendanceCode()));
    }

    @PostMapping("/attendance-code")
    public ResponseEntity<MessageResponse> setAttendanceCode(@RequestBody MessageResponse codeRequest, Authentication authentication) {
        Shop shop = getAuthShop(authentication);
        System.out.println("DEBUG: Setting Attendance Code");
        System.out.println("DEBUG: Shop ID: " + shop.getId());
        String newCode = (codeRequest.getMessage() != null) ? codeRequest.getMessage().trim() : null;
        System.out.println("DEBUG: New Code: " + newCode);
        
        shop.setAttendanceCode(newCode);
        shopRepository.save(shop);
        return ResponseEntity.ok(new MessageResponse("Attendance code updated successfully"));
    }

    // ---------------- Inventory ----------------

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(Authentication authentication) {

        Shop shop = getAuthShop(authentication);

        return ResponseEntity.ok(productRepository.findByShop_Id(shop.getId()));
    }

    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@RequestBody Product product, Authentication authentication) {

        Shop shop = getAuthShop(authentication);

        product.setShop(shop);
        product.setCreatedAt(LocalDateTime.now());

        productRepository.save(product);

        return ResponseEntity.ok(new MessageResponse("Product added successfully"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {

        productRepository.deleteById(id);

        return ResponseEntity.ok(new MessageResponse("Product deleted successfully"));
    }

    // ---------------- Staff ----------------

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffList(Authentication authentication) {

        Shop shop = getAuthShop(authentication);

        return ResponseEntity.ok(staffRepository.findByShop_Id(shop.getId()));
    }

    @PostMapping("/staff")
    public ResponseEntity<?> addStaff(@RequestBody User staffUserRequest, Authentication authentication) {

        Shop shop = getAuthShop(authentication);

        if (userRepository.existsByUsername(staffUserRequest.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // Create user
        User user = new User();
        user.setUsername(staffUserRequest.getUsername());
        user.setEmail(staffUserRequest.getEmail());
        user.setPhone(staffUserRequest.getPhone());
        user.setPassword(encoder.encode(staffUserRequest.getPassword()));
        user.setRole("STAFF");
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Create staff profile
        Staff staff = new Staff();
        staff.setUser(savedUser);
        staff.setShop(shop);
        staff.setName(staffUserRequest.getUsername());
        staff.setCreatedAt(LocalDateTime.now());

        staffRepository.save(staff);

        return ResponseEntity.ok(new MessageResponse("Staff member added successfully"));
    }

    // ---------------- Attendance ----------------

    @GetMapping("/attendance/{date}")
    public ResponseEntity<?> getAttendanceForDate(@PathVariable String date, Authentication authentication) {

        Shop shop = getAuthShop(authentication);

        LocalDate attendanceDate = LocalDate.parse(date);

        return ResponseEntity.ok(
                attendanceRepository.findByShop_IdAndDate(shop.getId(), attendanceDate)
        );
    }

}