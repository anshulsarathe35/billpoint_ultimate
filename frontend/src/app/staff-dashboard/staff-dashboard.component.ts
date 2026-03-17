import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff-dashboard.component.html',
  styleUrl: './staff-dashboard.component.scss'
})
export class StaffDashboardComponent implements OnInit {
  activeTab = 'billing'; // billing, customers, attendance
  
  // Billing
  customerPhoneSearch = '';
  selectedCustomer: any = null;
  products: any[] = [];
  cart: any[] = [];
  paymentMode = 'CASH';
  discount = 0;

  // New Customer
  newCustomer: any = {};

  private apiUrl = 'http://localhost:8080/api';

  constructor(public authService: AuthService, private http: HttpClient) {}

  ngOnInit(): void {
    this.loadAttendance();
    // Use dedicated staff endpoint for inventory access
    this.http.get<any[]>(`${this.apiUrl}/staff/products`).subscribe({
      next: (data) => this.products = data,
      error: () => console.log('Error loading products for staff.')
    });
  }

  // --- Attendance ---
  attendance = {
    date: new Date().toISOString().split('T')[0],
    workingHours: 0
  };
  attendanceHistory: any[] = [];

  loadAttendance() {
    const userRole = localStorage.getItem('userRole');
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (userRole === 'STAFF' && user.id) {
      // Find staffId from userId
      this.http.get<any>(`${this.apiUrl}/staff/profile`).subscribe({
        next: (staff) => {
          this.http.get<any[]>(`${this.apiUrl}/attendance/staff/${staff.id}`).subscribe(data => {
            this.attendanceHistory = data;
          });
        },
        error: () => console.error('Could not load staff profile')
      });
    }
  }

  markAttendance() {
    this.http.get<any>(`${this.apiUrl}/staff/profile`).subscribe({
      next: (staff) => {
        const payload = {
          staff: { id: staff.id },
          date: this.attendance.date,
          workingHours: this.attendance.workingHours
        };
        this.http.post(`${this.apiUrl}/attendance/mark`, payload).subscribe({
          next: (res: any) => {
            alert(res.message);
            this.loadAttendance();
          },
          error: (err) => alert(err.error?.message || 'Error marking attendance')
        });
      }
    });
  }

  // --- Customers ---
  searchCustomer() {
    this.http.get<any>(`${this.apiUrl}/staff/customers/search?phone=${this.customerPhoneSearch}`).subscribe({
      next: (data) => {
        this.selectedCustomer = data;
        alert('Customer found: ' + data.name);
      },
      error: () => alert('Customer not found')
    });
  }

  addCustomer() {
    this.http.post<any>(`${this.apiUrl}/staff/customers`, this.newCustomer).subscribe({
      next: (data) => {
        alert('Customer registered successfully!');
        this.selectedCustomer = data;
        this.newCustomer = {};
        this.activeTab = 'billing'; // go back to billing
      },
      error: (err) => alert('Error: ' + err.error?.message)
    });
  }

  // --- Billing ---
  addToCart(product: any) {
    const item = this.cart.find(i => i.product.id === product.id);
    if (item) {
      item.quantity += 1;
      item.totalPrice = item.quantity * item.pricePerUnit;
    } else {
      this.cart.push({
        product: { id: product.id, name: product.name },
        quantity: 1,
        pricePerUnit: product.price,
        totalPrice: product.price
      });
    }
  }

  removeFromCart(index: number) {
    this.cart.splice(index, 1);
  }

  get cartTotal() {
    return this.cart.reduce((sum, item) => sum + item.totalPrice, 0);
  }

  get finalAmount() {
    return this.cartTotal - this.discount;
  }

  generateBill() {
    if (!this.selectedCustomer) {
      alert('Please select or create a customer first.');
      return;
    }
    if (this.cart.length === 0) {
      alert('Cart is empty.');
      return;
    }

    const payload = {
      customerId: this.selectedCustomer.id,
      totalAmount: this.cartTotal,
      discount: this.discount,
      finalAmount: this.finalAmount,
      paymentMode: this.paymentMode,
      items: this.cart
    };

    this.http.post(`${this.apiUrl}/staff/bills`, payload).subscribe({
      next: (res: any) => {
        alert(res.message);
        this.cart = [];
        this.selectedCustomer = null;
        this.customerPhoneSearch = '';
        this.discount = 0;
      },
      error: (err) => alert('Error creating bill: ' + err.error?.message)
    });
  }

  logout() {
    this.authService.logout();
  }
}
