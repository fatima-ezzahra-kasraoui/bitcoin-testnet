import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { BitcoinService } from '../../services/bitcoin';
import Chart from 'chart.js/auto';
import { jsPDF } from 'jspdf';

@Component({
  selector: 'app-security',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './security.html',
  styleUrl: './security.css'
})
export class SecurityComponent implements OnInit, OnDestroy {
  @ViewChild('alertsChart') alertsChartRef!: ElementRef;
  @ViewChild('rulesChart') rulesChartRef!: ElementRef;

  insights: any = null;
  loading = true;
  isGeneratingPdf = false;

  private alertsChart: Chart | null = null;
  private rulesChart: Chart | null = null;

  constructor(private bitcoinService: BitcoinService, private router: Router) {}

  ngOnInit() {
    this.bitcoinService.getSecurityInsights().subscribe({
      next: (res) => {
        this.insights = res;
        this.loading = false;
        if (res.flaggedTransactions > 0) {
          setTimeout(() => this.initCharts(), 150);
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  ngOnDestroy() {
    this.alertsChart?.destroy();
    this.rulesChart?.destroy();
  }

  get ringColor(): string {
    if (!this.insights) return '#3B6D11';
    const s = this.insights.riskScore;
    if (s >= 75) return '#A32D2D';
    if (s >= 50) return '#854F0B';
    return '#3B6D11';
  }

  get ringDash(): string {
    if (!this.insights) return '0 339';
    const s = Math.min(Math.max(this.insights.riskScore, 0), 100);
    return `${(s * 3.39).toFixed(1)} 339`;
  }

  get scoreLabel(): string {
    if (!this.insights) return 'LOW RISK';
    if (this.insights.scoreLevel === 'HIGH') return 'HIGH RISK';
    if (this.insights.scoreLevel === 'MEDIUM') return 'MEDIUM RISK';
    return 'LOW RISK';
  }

  getFlaggedPercent(): string {
    if (!this.insights || this.insights.totalTransactions === 0) return '0';
    return ((this.insights.flaggedTransactions / this.insights.totalTransactions) * 100).toFixed(0);
  }

  async generatePdfReport() {
    if (!this.insights) return;
    this.isGeneratingPdf = true;

    try {
      const doc = new jsPDF('p', 'mm', 'a4');
      const pageWidth = doc.internal.pageSize.getWidth();

      // ── PAGE 1: Summary ─────────────────────────────────────

      // Header bar
      doc.setFillColor(20, 20, 30);
      doc.rect(0, 0, pageWidth, 40, 'F');

      doc.setTextColor(255, 255, 255);
      doc.setFontSize(22);
      doc.setFont('helvetica', 'bold');
      doc.text('Bitcoin TestNet', 14, 18);
      doc.setFontSize(14);
      doc.setFont('helvetica', 'normal');
      doc.text('Security Report', 14, 28);

      doc.setFontSize(9);
      doc.setTextColor(180, 180, 180);
      doc.text('Generated: ' + new Date().toLocaleString(), pageWidth - 14, 28, { align: 'right' });

      // Risk score section title
      doc.setTextColor(30, 30, 30);
      doc.setFontSize(12);
      doc.setFont('helvetica', 'bold');
      doc.text('RISK OVERVIEW', 14, 55);

      // Score colour box
      const scoreColor: [number, number, number] = this.insights.riskScore < 50
        ? [59, 109, 17]
        : this.insights.riskScore < 75
          ? [133, 79, 11]
          : [163, 45, 45];
      doc.setFillColor(scoreColor[0], scoreColor[1], scoreColor[2]);
      doc.roundedRect(14, 60, 60, 30, 4, 4, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(24);
      doc.setFont('helvetica', 'bold');
      doc.text(String(this.insights.riskScore), 44, 78, { align: 'center' });
      doc.setFontSize(9);
      doc.text(this.insights.scoreLevel + ' RISK', 44, 86, { align: 'center' });

      // Stat boxes
      doc.setTextColor(30, 30, 30);
      doc.setFontSize(10);
      doc.setFont('helvetica', 'normal');

      const stats = [
        { label: 'Total Transactions',  value: String(this.insights.totalTransactions) },
        { label: 'Flagged Transactions', value: String(this.insights.flaggedTransactions) },
        { label: 'Most Triggered Rule',  value: this.insights.mostFrequentRule || 'None' },
        { label: 'Last Alert',
          value: this.insights.lastAlertDate
            ? new Date(this.insights.lastAlertDate).toLocaleDateString()
            : 'None' }
      ];

      stats.forEach((stat, i) => {
        const x = 82 + (i % 2) * 62;
        const y = 60 + Math.floor(i / 2) * 18;
        doc.setFillColor(245, 245, 250);
        doc.roundedRect(x, y, 58, 14, 2, 2, 'F');
        doc.setFontSize(7);
        doc.setTextColor(120, 120, 120);
        doc.text(stat.label, x + 4, y + 5);
        doc.setFontSize(10);
        doc.setTextColor(30, 30, 30);
        doc.setFont('helvetica', 'bold');
        doc.text(stat.value, x + 4, y + 11);
        doc.setFont('helvetica', 'normal');
      });

      // Rules breakdown table
      doc.setTextColor(30, 30, 30);
      doc.setFontSize(12);
      doc.setFont('helvetica', 'bold');
      doc.text('RULES BREAKDOWN', 14, 105);

      const rules = Object.entries(this.insights.rulesBreakdown || {});
      const tableTop = 110;

      doc.setFillColor(20, 20, 30);
      doc.rect(14, tableTop, pageWidth - 28, 8, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(9);
      doc.text('Rule', 18, tableTop + 5.5);
      doc.text('Times Triggered', pageWidth - 18, tableTop + 5.5, { align: 'right' });

      rules.forEach(([rule, count], i) => {
        const rowY = tableTop + 8 + i * 9;
        doc.setFillColor(i % 2 === 0 ? 250 : 240, i % 2 === 0 ? 250 : 240, i % 2 === 0 ? 255 : 250);
        doc.rect(14, rowY, pageWidth - 28, 9, 'F');
        doc.setTextColor(30, 30, 30);
        doc.setFont('helvetica', 'normal');
        doc.text(rule, 18, rowY + 6);
        doc.setFont('helvetica', 'bold');
        doc.text(String(count), pageWidth - 18, rowY + 6, { align: 'right' });
        doc.setFont('helvetica', 'normal');
      });

      // Top risky transactions table
      const txTableTop = tableTop + 8 + rules.length * 9 + 15;
      doc.setTextColor(30, 30, 30);
      doc.setFontSize(12);
      doc.setFont('helvetica', 'bold');
      doc.text('TOP RISKY TRANSACTIONS', 14, txTableTop - 5);

      doc.setFillColor(20, 20, 30);
      doc.rect(14, txTableTop, pageWidth - 28, 8, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(9);
      doc.text('Transaction ID', 18, txTableTop + 5.5);
      doc.text('Risk Score', 100, txTableTop + 5.5);
      doc.text('Rules', 130, txTableTop + 5.5);
      doc.text('Date', pageWidth - 18, txTableTop + 5.5, { align: 'right' });

      (this.insights.topRiskyTransactions || []).forEach((tx: any, i: number) => {
        const rowY = txTableTop + 8 + i * 9;
        doc.setFillColor(i % 2 === 0 ? 250 : 240, i % 2 === 0 ? 250 : 240, i % 2 === 0 ? 255 : 250);
        doc.rect(14, rowY, pageWidth - 28, 9, 'F');
        doc.setTextColor(30, 30, 30);
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(8);
        const txIdShort = tx.txId ? tx.txId.substring(0, 16) + '...' : '—';
        doc.text(txIdShort, 18, rowY + 6);
        doc.text(String(tx.riskScore), 100, rowY + 6);
        const rulesStr = (tx.triggeredRules || []).join(', ');
        doc.text(rulesStr.length > 22 ? rulesStr.substring(0, 22) + '…' : rulesStr, 130, rowY + 6);
        doc.text(tx.date ? new Date(tx.date).toLocaleDateString() : '—', pageWidth - 18, rowY + 6, { align: 'right' });
      });

      // ── PAGE 2: Security Tips ────────────────────────────────

      doc.addPage();

      doc.setFillColor(20, 20, 30);
      doc.rect(0, 0, pageWidth, 20, 'F');
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(12);
      doc.text('Security Tips & Recommendations', 14, 13);

      doc.setTextColor(30, 30, 30);
      let tipY = 35;

      (this.insights.securityTips || []).forEach((tip: string) => {
        doc.setFillColor(250, 245, 230);
        doc.rect(14, tipY, pageWidth - 28, 16, 'F');
        doc.setFillColor(133, 79, 11);
        doc.rect(14, tipY, 3, 16, 'F');
        doc.setTextColor(80, 60, 10);
        doc.setFontSize(9);
        doc.setFont('helvetica', 'bold');
        doc.text('!', 20, tipY + 7);
        doc.setFont('helvetica', 'normal');
        // Clip long tips to page width
        const maxWidth = pageWidth - 44;
        const lines = doc.splitTextToSize(tip, maxWidth);
        doc.text(lines[0] || tip, 27, tipY + 7);
        tipY += 22;
      });

      // ── Footer on every page ─────────────────────────────────

      const pageCount = doc.getNumberOfPages();
      for (let p = 1; p <= pageCount; p++) {
        doc.setPage(p);
        doc.setFontSize(8);
        doc.setTextColor(150, 150, 150);
        doc.text(
          'Bitcoin TestNet Security Report — Confidential — Page ' + p + ' of ' + pageCount,
          pageWidth / 2,
          doc.internal.pageSize.getHeight() - 8,
          { align: 'center' }
        );
      }

      const filename = 'security-report-' + new Date().toISOString().split('T')[0] + '.pdf';
      doc.save(filename);
    } finally {
      this.isGeneratingPdf = false;
    }
  }

  private initCharts() {
    this.initAlertsChart();
    this.initRulesChart();
  }

  private initAlertsChart() {
    if (!this.alertsChartRef?.nativeElement) return;
    const labels: string[] = this.insights.alertsOverTime.map((d: any) => d.date.slice(5));
    const data: number[] = this.insights.alertsOverTime.map((d: any) => d.count);
    const colors = data.map((c: number) => c > 0 ? '#ef4444' : '#2a2a2a');

    this.alertsChart?.destroy();
    this.alertsChart = new Chart(this.alertsChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Alerts',
          data,
          backgroundColor: colors,
          borderRadius: 4,
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: '#888', font: { size: 11 } }, grid: { color: '#1a1a1a' } },
          y: { ticks: { color: '#888', stepSize: 1 }, grid: { color: '#1a1a1a' }, beginAtZero: true }
        }
      }
    });
  }

  private initRulesChart() {
    if (!this.rulesChartRef?.nativeElement || !this.insights.rulesBreakdown) return;
    const rules = Object.keys(this.insights.rulesBreakdown);
    const counts = Object.values(this.insights.rulesBreakdown) as number[];
    const ruleColors = ['#ef4444', '#f7931a', '#854F0B', '#3B6D11', '#8b5cf6'];

    this.rulesChart?.destroy();
    this.rulesChart = new Chart(this.rulesChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels: rules,
        datasets: [{
          label: 'Times Triggered',
          data: counts,
          backgroundColor: ruleColors,
          borderRadius: 4,
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { color: '#888', stepSize: 1 }, grid: { color: '#1a1a1a' }, beginAtZero: true },
          y: { ticks: { color: '#ccc', font: { size: 11 } }, grid: { color: '#1a1a1a' } }
        }
      }
    });
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}
