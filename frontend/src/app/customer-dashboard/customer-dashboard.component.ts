import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-customer-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './customer-dashboard.component.html',
  styleUrl: './customer-dashboard.component.scss'
})
export class CustomerDashboardComponent implements OnInit {
  activeTab = 'history'; // history, offers
  bills: any[] = [];
  offers: any[] = [];

  private apiUrl = 'http://localhost:8080/api/customer';

  constructor(public authService: AuthService, private http: HttpClient) {}

  ngOnInit(): void {
    this.loadBills();
    this.loadOffers();
  }

  loadBills() {
    this.http.get<any[]>(`${this.apiUrl}/bills`).subscribe({
      next: (data) => this.bills = data,
      error: (err) => console.error('Error loading bills:', err)
    });
  }

  loadOffers() {
    this.http.get<any[]>(`${this.apiUrl}/offers`).subscribe({
      next: (data) => this.offers = data,
      error: (err) => console.error('Error loading offers:', err)
    });
  }

  downloadInvoice(billId: number) {
    this.http.get(`${this.apiUrl}/bills/${billId}/invoice`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `invoice_${billId}.pdf`;
        a.click();
      },
      error: (err) => alert('Error downloading invoice: ' + err.error?.message)
    });
  }

  logout() {
    this.authService.logout();
  }
}
