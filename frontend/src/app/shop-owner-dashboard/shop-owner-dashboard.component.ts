import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-shop-owner-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './shop-owner-dashboard.component.html',
  styleUrl: './shop-owner-dashboard.component.scss'
})
export class ShopOwnerDashboardComponent implements OnInit {
  activeTab = 'inventory'; // inventory, staff, offers, reports
  
  // Data arrays
  products: any[] = [];
  staffList: any[] = [];
  offers: any[] = [];
  revenue: string = '0.00';
  shopName: string = '';
  attendanceCode: string = '';

  // Forms
  newProduct: any = {};
  newStaff: any = {};
  newOffer: any = {};

  private apiUrl = 'http://localhost:8080/api';

  constructor(public authService: AuthService, private http: HttpClient) {}

  ngOnInit(): void {
    this.loadShopProfile();
    this.loadProducts();
    this.loadStaff();
    this.loadOffers();
    this.loadRevenue();
    this.loadAttendanceCode();
  }

  loadShopProfile() {
    this.http.get<any>(`${this.apiUrl}/shop-owner/profile`).subscribe(data => this.shopName = data.name);
  }

  loadAttendanceCode() {
    this.http.get<any>(`${this.apiUrl}/shop-owner/attendance-code`).subscribe(res => this.attendanceCode = res.message);
  }

  updateAttendanceCode() {
    this.http.post(`${this.apiUrl}/shop-owner/attendance-code`, { message: this.attendanceCode }).subscribe(() => {
      alert('Attendance code updated');
    });
  }

  // --- Inventory ---
  loadProducts() {
    this.http.get<any[]>(`${this.apiUrl}/shop-owner/products`).subscribe(data => this.products = data);
  }

  addProduct() {
    this.http.post(`${this.apiUrl}/shop-owner/products`, this.newProduct).subscribe(() => {
      alert('Product added');
      this.newProduct = {};
      this.loadProducts();
    });
  }

  deleteProduct(id: number) {
    if(confirm('Delete product?')) {
      this.http.delete(`${this.apiUrl}/shop-owner/products/${id}`).subscribe(() => this.loadProducts());
    }
  }

  // --- Staff ---
  loadStaff() {
    this.http.get<any[]>(`${this.apiUrl}/shop-owner/staff`).subscribe(data => this.staffList = data);
  }

  addStaff() {
    this.http.post(`${this.apiUrl}/shop-owner/staff`, this.newStaff).subscribe({
      next: () => {
        alert('Staff added successfully');
        this.newStaff = {};
        this.loadStaff();
      },
      error: (err) => alert('Error adding staff: ' + err.error?.message)
    });
  }

  // --- Offers ---
  loadOffers() {
    this.http.get<any[]>(`${this.apiUrl}/offers`).subscribe(data => this.offers = data);
  }

  addOffer() {
    this.http.post(`${this.apiUrl}/offers`, this.newOffer).subscribe(() => {
      alert('Offer added');
      this.newOffer = {};
      this.loadOffers();
    });
  }

  deactivateOffer(id: number) {
    this.http.delete(`${this.apiUrl}/offers/${id}`).subscribe(() => this.loadOffers());
  }

  // --- Reports ---
  reportStartDate = new Date().toISOString().split('T')[0];
  reportEndDate = new Date().toISOString().split('T')[0];
  salesReportData: any[] = [];
  inventoryReportData: any[] = [];

  loadRevenue() {
    this.http.get<any>(`${this.apiUrl}/reports/revenue`).subscribe(res => {
      this.revenue = res.message.replace('Total Revenue: ', '');
    });
  }

  fetchSalesReport() {
    this.http.get<any[]>(`${this.apiUrl}/reports/sales?startDate=${this.reportStartDate}&endDate=${this.reportEndDate}`).subscribe(data => {
      this.salesReportData = data;
    });
  }

  fetchInventoryReport() {
    this.http.get<any[]>(`${this.apiUrl}/reports/inventory`).subscribe(data => {
      this.inventoryReportData = data;
    });
  }

  downloadDateReport() {
    this.http.get(`${this.apiUrl}/reports/download/sales?startDate=${this.reportStartDate}&endDate=${this.reportEndDate}`, { responseType: 'blob' }).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `sales_report_${this.reportStartDate}_to_${this.reportEndDate}.pdf`;
      a.click();
    });
  }

  logout() {
    this.authService.logout();
  }
}
