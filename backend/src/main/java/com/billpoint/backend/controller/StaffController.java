package com.billpoint.backend.controller;

import com.billpoint.backend.dto.BillRequest;
import com.billpoint.backend.dto.MessageResponse;
import com.billpoint.backend.model.*;
import com.billpoint.backend.repository.*;
import com.billpoint.backend.security.UserDetailsImpl;
import com.billpoint.backend.service.InvoiceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasRole('STAFF')")
public class StaffController {

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillItemRepository billItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InvoiceService invoiceService;

    private Staff getAuthStaff(Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        Staff staff = staffRepository.findByUser_Id(userDetails.getId());

        if (staff == null)
            throw new RuntimeException("Error: Staff not found for this user");

        return staff;
    }

    @GetMapping("/profile")
    public ResponseEntity<Staff> getStaffProfile(Authentication authentication) {
        Staff staff = getAuthStaff(authentication);
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(Authentication authentication) {
        Staff staff = getAuthStaff(authentication);
        return ResponseEntity.ok(productRepository.findByShop_Id(staff.getShop().getId()));
    }

    // ---------------- Attendance ----------------

    @PostMapping("/attendance/check-in")
    public ResponseEntity<?> checkIn(Authentication authentication) {

        Staff staff = getAuthStaff(authentication);

        LocalDate today = LocalDate.now();

        Attendance existing =
                attendanceRepository.findByStaff_IdAndDate(staff.getId(), today);

        if (existing != null && existing.getCheckIn() != null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Already checked in today."));
        }

        Attendance attendance = existing != null ? existing : new Attendance();

        attendance.setStaff(staff);
        attendance.setShop(staff.getShop());
        attendance.setDate(today);
        attendance.setCheckIn(LocalTime.now());
        attendance.setStatus("PRESENT");

        attendanceRepository.save(attendance);

        return ResponseEntity.ok(
                new MessageResponse("Checked in successfully at " + LocalTime.now()));
    }

    @PostMapping("/attendance/check-out")
    public ResponseEntity<?> checkOut(Authentication authentication) {

        Staff staff = getAuthStaff(authentication);

        LocalDate today = LocalDate.now();

        Attendance existing =
                attendanceRepository.findByStaff_IdAndDate(staff.getId(), today);

        if (existing == null || existing.getCheckIn() == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Must check in first."));
        }

        if (existing.getCheckOut() != null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Already checked out today."));
        }

        existing.setCheckOut(LocalTime.now());

        attendanceRepository.save(existing);

        return ResponseEntity.ok(
                new MessageResponse("Checked out successfully at " + LocalTime.now()));
    }

    // ---------------- Customer ----------------

    @GetMapping("/customers/search")
    public ResponseEntity<?> searchCustomerByPhone(
            @RequestParam String phone,
            Authentication authentication) {

        Staff staff = getAuthStaff(authentication);

        Optional<Customer> customer =
                customerRepository.findByPhoneAndShop_Id(phone, staff.getShop().getId());

        return customer.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/customers")
    public ResponseEntity<?> addCustomer(
            @RequestBody Customer customer,
            Authentication authentication) {

        Staff staff = getAuthStaff(authentication);

        Optional<Customer> existing =
                customerRepository.findByPhoneAndShop_Id(
                        customer.getPhone(),
                        staff.getShop().getId());

        if (existing.isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(
                            "Error: Customer with this phone already exists in this shop."));
        }

        customer.setShop(staff.getShop());
        customer.setCreatedAt(LocalDateTime.now());

        Customer savedCustomer = customerRepository.save(customer);

        return ResponseEntity.ok(savedCustomer);
    }

    // ---------------- Billing ----------------

    @PostMapping("/bills")
    public ResponseEntity<?> createBill(
            @RequestBody BillRequest billRequest,
            Authentication authentication) {

        Staff staff = getAuthStaff(authentication);

        Bill bill = new Bill();
        bill.setShop(staff.getShop());
        bill.setStaff(staff);

        if (billRequest.getCustomerId() != null) {
            Customer customer =
                    customerRepository.findById(billRequest.getCustomerId())
                            .orElse(null);
            bill.setCustomer(customer);
        }

        bill.setTotalAmount(billRequest.getTotalAmount());
        bill.setDiscount(
                billRequest.getDiscount() != null
                        ? billRequest.getDiscount()
                        : java.math.BigDecimal.ZERO);

        bill.setFinalAmount(billRequest.getFinalAmount());
        bill.setPaymentMode(billRequest.getPaymentMode());
        bill.setCreatedAt(LocalDateTime.now());

        Bill savedBill = billRepository.save(bill);

        // save bill items
        for (BillItem item : billRequest.getItems()) {

            Product product =
                    productRepository.findById(item.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("Product not found"));

            // 🔥 STOCK VALIDATION
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() 
                    + ". Available: " + product.getStockQuantity());
            }

            item.setBill(savedBill);
            item.setProduct(product);

            billItemRepository.save(item);

            // update stock
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());

            productRepository.save(product);
        }

        return ResponseEntity.ok(
                new MessageResponse(
                        "Bill created successfully. Bill ID: " + savedBill.getId()));
    }

    @GetMapping("/bills/{id}/invoice")
    public ResponseEntity<byte[]> getInvoice(@PathVariable Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        byte[] pdf = invoiceService.generateInvoicePdf(bill);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=invoice_" + id + ".pdf")
                .body(pdf);
    }
}