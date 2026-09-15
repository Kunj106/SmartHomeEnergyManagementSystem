const API_BASE_URL = 'https://energymanagementbackend.onrender.com/api';
const TOKEN_KEY = 'smart_energy_token';
const USER_KEY = 'smart_energy_user';

// ==================== ROLE UTILITIES ====================
const RoleUtils = {
  extractRole(roles) {
    if (!roles) return 'homeowner';
    let roleValue = Array.isArray(roles) ? roles[0] : roles;
    let roleStr = String(roleValue).toUpperCase().trim();
    if (roleStr.startsWith('ROLE_')) {
      roleStr = roleStr.substring(5);
    }
    roleStr = roleStr.toLowerCase();
    if (['admin', 'homeowner', 'technician'].includes(roleStr)) {
      return roleStr;
    }
    return 'homeowner';
  },

  getDashboardPage(roles) {
    const role = this.extractRole(roles);
    return `${role}.html`;
  },

  hasRole(roles, checkRole) {
    const role = this.extractRole(roles);
    return role === checkRole.toLowerCase();
  }
};

// ==================== AUTH MANAGER ====================
class AuthManager {
  static saveToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  static getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  static saveUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  static getUser() {
    const user = localStorage.getItem(USER_KEY);
    return user ? JSON.parse(user) : null;
  }

  static logout() {
    document.body.style.animation = 'fadeOut 0.5s ease';
    setTimeout(() => {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      window.location.href = 'login.html';
    }, 500);
  }

  static isAuthenticated() {
    return !!this.getToken();
  }

  static getAuthHeader() {
    const token = this.getToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }

  static getUserRole() {
    const user = this.getUser();
    return user ? RoleUtils.extractRole(user.roles) : null;
  }
}

// ==================== API SERVICE ====================
class APIService {
  static async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;

    const headers = {
      ...AuthManager.getAuthHeader(),
      ...(options.headers || {})
    };

    if (options.body && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    try {
      console.log(`API REQUEST: ${options.method || 'GET'} ${url}`);

      const response = await fetch(url, {
        ...options,
        headers
      });

      const contentType = response.headers.get('content-type') || '';
      let data = null;

      if (response.status !== 204) {
        if (contentType.includes('application/json')) {
          data = await response.json();
        } else {
          const text = await response.text();
          data = text || null;
        }
      }

      if (!response.ok) {
        let message = 'Request failed';

        if (data && typeof data === 'object') {
          message = data.message || data.error || message;
        } else if (typeof data === 'string' && data.trim()) {
          message = data;
        }

        if (response.status === 401) {
          message = message || 'Unauthorized. Please sign in again.';
        }

        throw new Error(message);
      }

      return data;

    } catch (error) {
      console.error('API Error:', { endpoint, url, message: error.message });
      throw error;
    }
  }

  // Auth endpoints
  static async signup(userData) {
    return this.request('/auth/signup', {
      method: 'POST',
      body: JSON.stringify(userData)
    });
  }

  static async signin(credentials) {
    return this.request('/auth/signin', {
      method: 'POST',
      body: JSON.stringify(credentials)
    });
  }

  static async getProfile() {
    return this.request('/profile', { method: 'GET' });
  }

  static async createProfile(profileData) {
    return this.request('/profile', {
      method: 'POST',
      body: JSON.stringify(profileData)
    });
  }

  static async getAllUsers() {
    return this.request('/admin/users', { method: 'GET' });
  }

  static async deleteUser(userId) {
    return this.request(`/admin/users/${userId}`, { method: 'DELETE' });
  }

  // Device endpoints
  static async getAllDevices() {
    return this.request('/devices', { method: 'GET' });
  }

  static async getDeviceById(deviceId) {
    return this.request(`/devices/${deviceId}`, { method: 'GET' });
  }

  static async createDevice(deviceData) {
    return this.request('/devices', {
      method: 'POST',
      body: JSON.stringify(deviceData)
    });
  }

  static async updateDevice(deviceId, deviceData) {
    return this.request(`/devices/${deviceId}`, {
      method: 'PUT',
      body: JSON.stringify(deviceData)
    });
  }

  static async deleteDevice(deviceId) {
    return this.request(`/devices/${deviceId}`, { method: 'DELETE' });
  }

  static async updateDeviceStatus(deviceId, status) {
    return this.request(`/devices/${deviceId}/status?status=${status}`, {
      method: 'PATCH'
    });
  }

  static async getDeviceStatistics() {
    return this.request('/devices/statistics', { method: 'GET' });
  }

  static async getActiveDevices() {
    return this.request('/devices/active', { method: 'GET' });
  }

  // Energy endpoints
  static async createEnergyLog(logData) {
    return this.request('/energy/logs', {
      method: 'POST',
      body: JSON.stringify(logData)
    });
  }

  static async getHourlyConsumption() {
    return this.request('/energy/graphs/hourly', { method: 'GET' });
  }

  static async getDailyConsumption() {
    return this.request('/energy/graphs/daily', { method: 'GET' });
  }

  static async getConsumptionByDevice() {
    return this.request('/energy/consumption/by-device', { method: 'GET' });
  }

  static async getConsumptionSummary() {
    return this.request('/energy/summary', { method: 'GET' });
  }

  static async generateHourlyLogs() {
    return this.request('/energy/logs/generate-hourly', { method: 'POST' });
  }

  static async generateDailySummary() {
    return this.request('/energy/logs/generate-daily', { method: 'POST' });
  }
}

// ==================== UI UTILITIES ====================
class UIUtils {
  static showAlert(message, type = 'success') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.innerHTML = `
      <strong>${type === 'success' ? '✓' : type === 'error' ? '✗' : '!'}</strong>
      ${message}
    `;

    const container = document.querySelector('.container') || document.body;
    container.insertBefore(alertDiv, container.firstChild);

    setTimeout(() => {
      alertDiv.style.animation = 'alertSlide 0.4s reverse';
      setTimeout(() => alertDiv.remove(), 400);
    }, 4000);
  }

  static showLoading(button) {
    const originalText = button.innerHTML;
    button.disabled = true;
    button.innerHTML = '<span class="loading"></span>';
    return originalText;
  }

  static hideLoading(button, originalText) {
    button.disabled = false;
    button.innerHTML = originalText;
  }

  static animateCounter(element, start, end, duration = 2000) {
    const startTime = performance.now();

    const animate = (currentTime) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const easeOutQuart = 1 - Math.pow(1 - progress, 4);
      const current = Math.floor(start + (end - start) * easeOutQuart);

      element.textContent = current.toLocaleString();

      if (progress < 1) {
        requestAnimationFrame(animate);
      } else {
        element.textContent = end.toLocaleString();
      }
    };

    requestAnimationFrame(animate);
  }

  static createParticles(element, count = 20) {
    const particles = [];
    const rect = element.getBoundingClientRect();

    for (let i = 0; i < count; i++) {
      const particle = document.createElement('div');
      particle.style.cssText = `
        position: fixed;
        width: 4px;
        height: 4px;
        background: var(--electric-cyan);
        border-radius: 50%;
        pointer-events: none;
        z-index: 9999;
        box-shadow: 0 0 10px var(--electric-cyan);
      `;

      particle.style.left = `${rect.left + rect.width / 2}px`;
      particle.style.top = `${rect.top + rect.height / 2}px`;

      document.body.appendChild(particle);
      particles.push(particle);

      const angle = (Math.PI * 2 * i) / count;
      const velocity = 2 + Math.random() * 3;
      const vx = Math.cos(angle) * velocity;
      const vy = Math.sin(angle) * velocity;

      let x = rect.left + rect.width / 2;
      let y = rect.top + rect.height / 2;
      let opacity = 1;

      const animate = () => {
        x += vx;
        y += vy;
        opacity -= 0.02;

        particle.style.left = `${x}px`;
        particle.style.top = `${y}px`;
        particle.style.opacity = opacity;

        if (opacity > 0) {
          requestAnimationFrame(animate);
        } else {
          particle.remove();
        }
      };

      requestAnimationFrame(animate);
    }
  }

  static formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  static formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

// ==================== DEVICE MANAGER ====================
class DeviceManager {
  static async loadDevices() {
    try {
      const devices = await APIService.getAllDevices();
      return devices;
    } catch (error) {
      console.error('Error loading devices:', error);
      UIUtils.showAlert('Failed to load devices: ' + error.message, 'error');
      return [];
    }
  }

  static async createDevice(deviceData) {
    try {
      const device = await APIService.createDevice(deviceData);
      UIUtils.showAlert('Device created successfully!', 'success');
      return device;
    } catch (error) {
      console.error('Error creating device:', error);
      UIUtils.showAlert('Failed to create device: ' + error.message, 'error');
      throw error;
    }
  }

  static async updateDevice(deviceId, deviceData) {
    try {
      const device = await APIService.updateDevice(deviceId, deviceData);
      UIUtils.showAlert('Device updated successfully!', 'success');
      return device;
    } catch (error) {
      console.error('Error updating device:', error);
      UIUtils.showAlert('Failed to update device: ' + error.message, 'error');
      throw error;
    }
  }

  static async deleteDevice(deviceId) {
    try {
      await APIService.deleteDevice(deviceId);
      UIUtils.showAlert('Device deleted successfully!', 'success');
    } catch (error) {
      console.error('Error deleting device:', error);
      UIUtils.showAlert('Failed to delete device: ' + error.message, 'error');
      throw error;
    }
  }

  static async toggleDeviceStatus(deviceId, currentStatus) {
    const newStatus = currentStatus === 'online' ? 'offline' : 'online';
    try {
      await APIService.updateDeviceStatus(deviceId, newStatus);
      UIUtils.showAlert(`Device turned ${newStatus}!`, 'success');
      return newStatus;
    } catch (error) {
      console.error('Error updating status:', error);
      UIUtils.showAlert('Failed to update status: ' + error.message, 'error');
      throw error;
    }
  }
}

// ==================== MOCK DATA ====================
const MockData = {
  hourly: [
    { label: '6 AM',  value: 0.42 },
    { label: '7 AM',  value: 0.78 },
    { label: '8 AM',  value: 1.15 },
    { label: '9 AM',  value: 0.95 },
    { label: '10 AM', value: 1.32 },
    { label: '11 AM', value: 1.08 },
    { label: '12 PM', value: 0.87 }
  ],
  daily: [
    { label: 'Feb 20', value: 8.4  },
    { label: 'Feb 21', value: 11.2 },
    { label: 'Feb 22', value: 9.7  },
    { label: 'Feb 23', value: 13.5 },
    { label: 'Feb 24', value: 10.1 },
    { label: 'Feb 25', value: 12.8 },
    { label: 'Feb 26', value: 9.3  }
  ]
};

// ==================== CHART MANAGER ====================
class ChartManager {
  // CHART 1: Hourly Consumption (6 AM - 12 PM, Curved)
  static async createHourlyChart(canvasId) {
    try {
      let chartData = [];
      let usingMock = false;

      try {
        const allData = await APIService.getHourlyConsumption();
        console.log('Hourly data from API:', JSON.stringify(allData));

        // Try filtering 6 AM–12 PM using multiple label format strategies
        let filtered = allData.filter(d => {
          const label = String(d.label || '');
          const hmMatch = label.match(/^(\d{1,2})[:h]/);
          if (hmMatch) {
            const hour = parseInt(hmMatch[1], 10);
            return hour >= 6 && hour <= 12;
          }
          const isoMatch = label.match(/T(\d{2}):/);
          if (isoMatch) {
            const hour = parseInt(isoMatch[1], 10);
            return hour >= 6 && hour <= 12;
          }
          const ampmMatch = label.match(/(\d{1,2})(?::(\d{2}))?\s*(AM|PM)/i);
          if (ampmMatch) {
            let hour = parseInt(ampmMatch[1], 10);
            const period = ampmMatch[3].toUpperCase();
            if (period === 'PM' && hour !== 12) hour += 12;
            if (period === 'AM' && hour === 12) hour = 0;
            return hour >= 6 && hour <= 12;
          }
          return false;
        });

        if (filtered.length === 0) filtered = allData;

        // Check if all values are zero — fall back to mock if so
        const allZero = filtered.every(d => !d.value || d.value === 0);
        if (!allZero && filtered.length > 0) {
          chartData = filtered;
        } else {
          usingMock = true;
          chartData = MockData.hourly;
        }
      } catch (apiErr) {
        console.warn('Hourly API failed, using mock data:', apiErr.message);
        usingMock = true;
        chartData = MockData.hourly;
      }

      if (usingMock) {
        console.log('Hourly chart: using mock data (API returned zeros or failed)');
      }

      const ctx = document.getElementById(canvasId);
      if (!ctx) return;

      return new Chart(ctx, {
        type: 'line',
        data: {
          labels: chartData.map(d => d.label),
          datasets: [{
            label: 'Energy Consumed (kWh)',
            data: chartData.map(d => d.value),
            borderColor: '#00F0FF',
            backgroundColor: 'rgba(0, 240, 255, 0.1)',
            tension: 0.5,
            fill: true,
            borderWidth: 3,
            pointRadius: 5,
            pointHoverRadius: 7,
            pointBackgroundColor: '#00F0FF',
            pointBorderColor: '#0B0D17',
            pointBorderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              labels: {
                color: '#FFFFFF',
                font: { size: 14, family: "'Montserrat', sans-serif" }
              }
            },
            title: {
              display: true,
              text: 'Morning Energy Usage (6 AM - 12 PM)',
              color: '#00F0FF',
              font: { size: 16, family: "'Orbitron', sans-serif" }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { color: '#B8C5D6', font: { size: 12 } },
              grid: { color: 'rgba(0, 240, 255, 0.1)' }
            },
            x: {
              ticks: { color: '#B8C5D6', font: { size: 12 } },
              grid: { color: 'rgba(0, 240, 255, 0.1)' }
            }
          }
        }
      });
    } catch (error) {
      console.error('Error creating hourly chart:', error);
    }
  }

  // CHART 2: Daily Consumption (Last 7 Days)
  static async createDailyChart(canvasId) {
    try {
      let last7Days = [];
      let usingMock = false;

      try {
        const allData = await APIService.getDailyConsumption();
        const slice = allData.slice(-7);

        // Check if all values are zero — fall back to mock if so
        const allZero = slice.every(d => !d.value || d.value === 0);
        if (!allZero && slice.length > 0) {
          last7Days = slice;
        } else {
          usingMock = true;
          last7Days = MockData.daily;
        }
      } catch (apiErr) {
        console.warn('Daily API failed, using mock data:', apiErr.message);
        usingMock = true;
        last7Days = MockData.daily;
      }

      if (usingMock) {
        console.log('Daily chart: using mock data (API returned zeros or failed)');
      }

      const ctx = document.getElementById(canvasId);
      if (!ctx) return;

      const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

      // If using real API data, regenerate sequential labels; mock data already has labels
      let labels;
      if (!usingMock) {
        let startDate = null;
        if (last7Days.length > 0 && last7Days[0].label) {
          const parsed = new Date(last7Days[0].label + ' 2025');
          if (!isNaN(parsed.getTime())) startDate = parsed;
        }
        if (!startDate) startDate = new Date(2025, 1, 11);
        labels = last7Days.map((_, i) => {
          const d = new Date(startDate);
          d.setDate(startDate.getDate() + i);
          return `${monthNames[d.getMonth()]} ${d.getDate()}`;
        });
      } else {
        labels = last7Days.map(d => d.label);
      }

      return new Chart(ctx, {
        type: 'bar',
        data: {
          labels: labels,
          datasets: [{
            label: 'Daily Energy (kWh)',
            data: last7Days.map(d => d.value),
            backgroundColor: 'rgba(0, 240, 255, 0.6)',
            borderColor: '#00F0FF',
            borderWidth: 2,
            borderRadius: 8,
            hoverBackgroundColor: 'rgba(0, 240, 255, 0.8)'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              labels: {
                color: '#FFFFFF',
                font: { size: 14, family: "'Montserrat', sans-serif" }
              }
            },
            title: {
              display: true,
              text: 'Last 7 Days Energy Consumption',
              color: '#00F0FF',
              font: { size: 16, family: "'Orbitron', sans-serif" }
            }
          },
          scales: {
            y: {
              beginAtZero: true,
              ticks: { color: '#B8C5D6', font: { size: 12 } },
              grid: { color: 'rgba(0, 240, 255, 0.1)' }
            },
            x: {
              ticks: { color: '#B8C5D6', font: { size: 12 } },
              grid: { color: 'rgba(0, 240, 255, 0.1)' }
            }
          }
        }
      });
    } catch (error) {
      console.error('Error creating daily chart:', error);
    }
  }

  // CHART 3: Per-Device Energy Consumption (Doughnut)
  static async createDeviceTypeChart(canvasId) {
    try {
      const devices = await APIService.getAllDevices();
      const ctx = document.getElementById(canvasId);

      if (!ctx) return;

      const activeDevices = devices.filter(d => d.powerRating && d.powerRating > 0);

      if (activeDevices.length === 0) {
        ctx.parentElement.innerHTML = '<p style="color:#B8C5D6;text-align:center;padding-top:2rem;">No device data available</p>';
        return;
      }

      const energyValues = activeDevices.map(d => parseFloat((d.powerRating / 1000 * 8).toFixed(3)));
      const totalEnergy = energyValues.reduce((sum, v) => sum + v, 0);

      const palette = [
        'rgba(0, 240, 255, 0.85)', 'rgba(0, 102, 255, 0.85)', 'rgba(255, 215, 0, 0.85)',
        'rgba(168, 85, 247, 0.85)', 'rgba(255, 107, 107, 0.85)', 'rgba(46, 213, 115, 0.85)',
        'rgba(255, 165, 0, 0.85)', 'rgba(255, 20, 147, 0.85)', 'rgba(0, 255, 127, 0.85)',
        'rgba(135, 206, 250, 0.85)', 'rgba(255, 99, 71, 0.85)', 'rgba(186, 225, 255, 0.85)'
      ];
      const hoverPalette = palette.map(c => c.replace('0.85', '1'));
      const colors = activeDevices.map((_, i) => palette[i % palette.length]);
      const hoverColors = activeDevices.map((_, i) => hoverPalette[i % hoverPalette.length]);
      const labels = activeDevices.map(d => `${d.name} (${d.type || d.deviceType || 'Device'})`);

      return new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: labels,
          datasets: [{
            data: energyValues,
            backgroundColor: colors,
            hoverBackgroundColor: hoverColors,
            borderColor: '#0B0D17',
            borderWidth: 3,
            hoverBorderWidth: 4
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'bottom',
              labels: {
                color: '#FFFFFF',
                font: { size: 12, family: "'Montserrat', sans-serif" },
                padding: 12,
                usePointStyle: true,
                pointStyle: 'circle'
              }
            },
            title: {
              display: true,
              text: 'Energy Consumption per Device (est. 8h/day)',
              color: '#00F0FF',
              font: { size: 16, family: "'Orbitron', sans-serif" },
              padding: { top: 10, bottom: 20 }
            },
            tooltip: {
              backgroundColor: 'rgba(11, 13, 23, 0.95)',
              titleColor: '#00F0FF',
              bodyColor: '#FFFFFF',
              borderColor: '#00F0FF',
              borderWidth: 2,
              padding: 12,
              displayColors: true,
              callbacks: {
                label: function(context) {
                  const value = context.parsed || 0;
                  const percentage = totalEnergy > 0 ? ((value / totalEnergy) * 100).toFixed(1) : 0;
                  const device = activeDevices[context.dataIndex];
                  return [
                    `Power: ${device.powerRating}W`,
                    `Est. daily: ${value.toFixed(3)} kWh (${percentage}%)`
                  ];
                }
              }
            }
          },
          cutout: '60%'
        }
      });
    } catch (error) {
      console.error('Error creating device type chart:', error);
    }
  }
}

// ==================== NAVIGATION ====================
function updateNavigation() {
  const user = AuthManager.getUser();
  const navMenu = document.querySelector('.nav-menu');

  if (!navMenu) return;

  if (user) {
    const role = RoleUtils.extractRole(user.roles);
    const dashboardLink = RoleUtils.getDashboardPage(user.roles);

    let navItems = `<li><a href="${dashboardLink}" class="nav-link">Dashboard</a></li>`;

    if (role === 'homeowner') {
      navItems += `
        <li><a href="devices.html" class="nav-link">Devices</a></li>
        <li><a href="energy.html" class="nav-link">Energy</a></li>
      `;
    }

    const profilePage = role === 'homeowner' ? 'homeownerprofile.html'
                      : role === 'admin'     ? 'adminprofile.html'
                      : role === 'technician'? 'technicianprofile.html'
                      : 'profile.html';

    navItems += `
      <li><a href="${profilePage}" class="nav-link">Profile</a></li>
      <li><a href="#" class="nav-link" onclick="AuthManager.logout(); return false;">Logout</a></li>
      <li>
        <span class="nav-link" style="color: var(--electric-cyan); cursor: default;">
          <span style="font-size: 1.2rem;">👤</span> ${user.username}
        </span>
      </li>
    `;

    navMenu.innerHTML = navItems;
  } else {
    navMenu.innerHTML = `
      <li><a href="login.html" class="btn btn-secondary">Login</a></li>
      <li><a href="register.html" class="btn btn-primary">Register</a></li>
    `;
  }
}

// ==================== ANIMATIONS ====================
function initializeAnimations() {
  const cards = document.querySelectorAll('.card');
  cards.forEach((card, index) => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(30px)';

    setTimeout(() => {
      card.style.transition = 'all 0.6s cubic-bezier(0.4, 0, 0.2, 1)';
      card.style.opacity = '1';
      card.style.transform = 'translateY(0)';
    }, index * 100);
  });

  const formGroups = document.querySelectorAll('.form-group');
  formGroups.forEach((group, index) => {
    group.style.animationDelay = `${index * 0.1}s`;
  });

  const statValues = document.querySelectorAll('.stat-value');
  statValues.forEach(stat => {
    const finalValue = parseInt(stat.textContent.replace(/,/g, ''));
    if (!isNaN(finalValue)) {
      stat.textContent = '0';
      setTimeout(() => {
        UIUtils.animateCounter(stat, 0, finalValue, 2000);
      }, 500);
    }
  });

  document.querySelectorAll('.btn-primary').forEach(button => {
    button.addEventListener('click', function(e) {
      if (!this.disabled) {
        UIUtils.createParticles(this, 15);
      }
    });
  });
}

// ==================== INITIALIZE ====================
document.addEventListener('DOMContentLoaded', () => {
  updateNavigation();
  initializeAnimations();
  document.body.style.animation = 'fadeIn 0.8s ease';
});

// Export globals
window.AuthManager = AuthManager;
window.APIService = APIService;
window.UIUtils = UIUtils;
window.DeviceManager = DeviceManager;
window.ChartManager = ChartManager;
window.RoleUtils = RoleUtils;
window.MockData = MockData;

// =============================================================
// ==================== ADMIN API ====================
// =============================================================

const AdminAPI = {

  // ── Manage Users ──────────────────────────────────────────

  async getAllUsers() {
    return APIService.request('/admin/users', { method: 'GET' });
  },

  async updateUser(userId, updates) {
    return APIService.request(`/admin/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(updates)
    });
  },

  async toggleUser(userId) {
    return APIService.request(`/admin/users/${userId}/toggle`, { method: 'PATCH' });
  },

  async deleteUser(userId) {
    return APIService.request(`/admin/users/${userId}`, { method: 'DELETE' });
  },

  // ── View Logs ─────────────────────────────────────────────

  async getLogs({ page = 0, size = 50, level = '', action = '' } = {}) {
    let qs = `?page=${page}&size=${size}`;
    if (level)  qs += `&level=${encodeURIComponent(level)}`;
    if (action) qs += `&action=${encodeURIComponent(action)}`;
    return APIService.request(`/admin/logs${qs}`, { method: 'GET' });
  },

  // ── Generate Report ───────────────────────────────────────

  async getReportJson() {
    return APIService.request('/admin/report/json', { method: 'GET' });
  },

  async downloadReportCsv() {
    const token = AuthManager.getToken();
    const response = await fetch(`${API_BASE_URL}/admin/report/csv`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Report download failed');
    const blob = await response.blob();
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `smart_energy_report_${new Date().toISOString().slice(0,10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  },

  // ── Security / 2FA ────────────────────────────────────────

  async changePassword(currentPassword, newPassword) {
    return APIService.request('/admin/security/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword })
    });
  },

  async get2FAStatus() {
    return APIService.request('/admin/security/2fa/status', { method: 'GET' });
  },

  async initiate2FA(phoneNumber) {
    return APIService.request('/admin/security/2fa/initiate', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber })
    });
  },

  async verify2FA(otp) {
    return APIService.request('/admin/security/2fa/verify', {
      method: 'POST',
      body: JSON.stringify({ otp })
    });
  },

  async disable2FA(currentPassword) {
    return APIService.request('/admin/security/2fa/disable', {
      method: 'POST',
      body: JSON.stringify({ currentPassword })
    });
  }
};

// ==================== QUICK ACTIONS MANAGER ====================

const QuickActions = {

  // ── Manage Users modal ──────────────────────────────────────

  async openManageUsers() {
    const existing = document.getElementById('manageUsersModal');
    if (existing) { existing.style.display = 'flex'; await QuickActions._loadUsersTable(); return; }
    QuickActions._buildManageUsersModal();
  },

  _buildManageUsersModal() {
    const overlay = document.createElement('div');
    overlay.id = 'manageUsersModal';
    overlay.className = 'qa-modal-overlay';
    overlay.innerHTML = `
      <div class="qa-modal">
        <div class="qa-modal-header">
          <h3>👥 MANAGE USERS</h3>
          <button class="qa-modal-close" onclick="QuickActions.closeModal('manageUsersModal')">✕</button>
        </div>
        <div class="qa-modal-body">
          <div id="usersTableWrap">
            <div class="qa-loading"><span class="loading"></span> Loading users…</div>
          </div>
        </div>
      </div>`;
    document.body.appendChild(overlay);
    overlay.style.display = 'flex';
    QuickActions._loadUsersTable();
  },

  async _loadUsersTable() {
    const wrap = document.getElementById('usersTableWrap');
    if (!wrap) return;
    try {
      const users = await AdminAPI.getAllUsers();
      wrap.innerHTML = `
        <table class="qa-table">
          <thead>
            <tr>
              <th>ID</th><th>USERNAME</th><th>EMAIL</th><th>ROLE</th><th>STATUS</th><th>ACTIONS</th>
            </tr>
          </thead>
          <tbody>
            ${users.map(u => `
              <tr id="user-row-${u.id}">
                <td>${u.id}</td>
                <td>${u.username}</td>
                <td>${u.email}</td>
                <td>${u.roles?.map(r => String(r.name || r).replace('ROLE_','')).join(', ') || '—'}</td>
                <td>
                  <span class="qa-badge ${u.enabled ? 'qa-badge-green' : 'qa-badge-red'}">
                    ${u.enabled ? '● ACTIVE' : '○ DISABLED'}
                  </span>
                </td>
                <td class="qa-actions">
                  <button class="qa-btn qa-btn-warn"
                          onclick="QuickActions.toggleUser(${u.id}, ${u.enabled})">
                    ${u.enabled ? 'DISABLE' : 'ENABLE'}
                  </button>
                  <button class="qa-btn qa-btn-danger"
                          onclick="QuickActions.deleteUser(${u.id}, '${u.username}')">
                    DELETE
                  </button>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>`;
    } catch (e) {
      wrap.innerHTML = `<div class="qa-error">Failed to load users: ${e.message}</div>`;
    }
  },

  async toggleUser(userId, currentEnabled) {
    if (!confirm(`${currentEnabled ? 'Disable' : 'Enable'} this user?`)) return;
    try {
      const res = await AdminAPI.toggleUser(userId);
      UIUtils.showAlert(`User ${res.enabled ? 'enabled' : 'disabled'} successfully.`, 'success');
      await QuickActions._loadUsersTable();
    } catch (e) {
      UIUtils.showAlert('Failed: ' + e.message, 'error');
    }
  },

  async deleteUser(userId, username) {
    if (!confirm(`Permanently delete user "${username}"? This cannot be undone.`)) return;
    try {
      await AdminAPI.deleteUser(userId);
      UIUtils.showAlert(`User "${username}" deleted.`, 'success');
      await QuickActions._loadUsersTable();
    } catch (e) {
      UIUtils.showAlert('Failed: ' + e.message, 'error');
    }
  },

  // ── View Logs modal ─────────────────────────────────────────

  async openViewLogs() {
    const existing = document.getElementById('viewLogsModal');
    if (existing) { existing.style.display = 'flex'; await QuickActions._loadLogs(); return; }

    const overlay = document.createElement('div');
    overlay.id = 'viewLogsModal';
    overlay.className = 'qa-modal-overlay';
    overlay.innerHTML = `
      <div class="qa-modal qa-modal-wide">
        <div class="qa-modal-header">
          <h3>📋 AUDIT LOGS</h3>
          <button class="qa-modal-close" onclick="QuickActions.closeModal('viewLogsModal')">✕</button>
        </div>
        <div class="qa-modal-body">
          <div class="qa-log-filters">
            <select id="logLevelFilter" onchange="QuickActions._loadLogs()">
              <option value="">All Levels</option>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
            </select>
            <input id="logActionFilter" placeholder="Filter by action…"
                   oninput="QuickActions._debouncedLoadLogs()" />
            <button class="qa-btn qa-btn-primary" onclick="QuickActions._loadLogs()">REFRESH</button>
          </div>
          <div id="logSummaryBar"></div>
          <div id="logsTableWrap">
            <div class="qa-loading"><span class="loading"></span> Loading logs…</div>
          </div>
          <div id="logsPagination" class="qa-pagination"></div>
        </div>
      </div>`;
    document.body.appendChild(overlay);
    overlay.style.display = 'flex';

    let debounceTimer;
    QuickActions._debouncedLoadLogs = () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => QuickActions._loadLogs(), 400);
    };

    await QuickActions._loadLogs();
  },

  _logsPage: 0,

  async _loadLogs(page = 0) {
    QuickActions._logsPage = page;
    const wrap    = document.getElementById('logsTableWrap');
    const pagDiv  = document.getElementById('logsPagination');
    const summary = document.getElementById('logSummaryBar');
    if (!wrap) return;

    const level  = document.getElementById('logLevelFilter')?.value || '';
    const action = document.getElementById('logActionFilter')?.value || '';

    wrap.innerHTML = `<div class="qa-loading"><span class="loading"></span> Loading…</div>`;

    try {
      const data = await AdminAPI.getLogs({ page, size: 20, level, action });
      const s = data.summary;

      if (summary) {
        summary.innerHTML = `
          <div class="qa-summary-bar">
            <span class="qa-badge qa-badge-blue">TOTAL ${s.total}</span>
            <span class="qa-badge qa-badge-green">INFO ${s.infoCount}</span>
            <span class="qa-badge qa-badge-yellow">WARN ${s.warnCount}</span>
            <span class="qa-badge qa-badge-red">ERROR ${s.errorCount}</span>
            <span class="qa-badge qa-badge-cyan">TODAY ${s.todayCount}</span>
          </div>`;
      }

      if (!data.content?.length) {
        wrap.innerHTML = '<div class="qa-empty">No log entries found.</div>';
        if (pagDiv) pagDiv.innerHTML = '';
        return;
      }

      wrap.innerHTML = `
        <table class="qa-table">
          <thead>
            <tr>
              <th>TIME</th><th>USER</th><th>ACTION</th>
              <th>DESCRIPTION</th><th>LEVEL</th><th>IP</th>
            </tr>
          </thead>
          <tbody>
            ${data.content.map(l => `
              <tr>
                <td class="qa-mono">${new Date(l.timestamp).toLocaleString()}</td>
                <td>${l.username}</td>
                <td class="qa-mono">${l.action}</td>
                <td>${l.description || '—'}</td>
                <td>
                  <span class="qa-badge qa-badge-${QuickActions._levelColor(l.level)}">
                    ${l.level}
                  </span>
                </td>
                <td class="qa-mono">${l.ipAddress || '—'}</td>
              </tr>`).join('')}
          </tbody>
        </table>`;

      if (pagDiv) {
        const total = data.totalPages;
        let btns = '';
        for (let i = 0; i < total; i++) {
          btns += `<button class="qa-btn ${i === page ? 'qa-btn-primary' : ''}"
                           onclick="QuickActions._loadLogs(${i})">${i + 1}</button>`;
        }
        pagDiv.innerHTML = `<div class="qa-pagination-inner">${btns}</div>`;
      }
    } catch (e) {
      wrap.innerHTML = `<div class="qa-error">Failed to load logs: ${e.message}</div>`;
    }
  },

  _levelColor(level) {
    return level === 'ERROR' ? 'red' : level === 'WARN' ? 'yellow' : 'green';
  },

  // ── Generate Report modal ───────────────────────────────────

  async openGenerateReport() {
    const existing = document.getElementById('reportModal');
    if (existing) { existing.style.display = 'flex'; return; }

    const overlay = document.createElement('div');
    overlay.id = 'reportModal';
    overlay.className = 'qa-modal-overlay';
    overlay.innerHTML = `
      <div class="qa-modal">
        <div class="qa-modal-header">
          <h3>📊 GENERATE REPORT</h3>
          <button class="qa-modal-close" onclick="QuickActions.closeModal('reportModal')">✕</button>
        </div>
        <div class="qa-modal-body">
          <div id="reportContent">
            <div class="qa-loading"><span class="loading"></span> Generating report…</div>
          </div>
          <div style="display:flex;gap:1rem;margin-top:1.5rem;flex-wrap:wrap;">
            <button class="btn btn-primary" onclick="QuickActions._downloadCsv()">
              ⬇ DOWNLOAD CSV
            </button>
            <button class="btn btn-secondary" onclick="QuickActions._refreshReport()">
              🔄 REFRESH
            </button>
          </div>
        </div>
      </div>`;
    document.body.appendChild(overlay);
    overlay.style.display = 'flex';
    await QuickActions._loadReportData();
  },

  async _loadReportData() {
    const wrap = document.getElementById('reportContent');
    if (!wrap) return;
    try {
      const r = await AdminAPI.getReportJson();
      wrap.innerHTML = `
        <div class="qa-report-grid">
          ${QuickActions._reportSection('📋 META', r.meta)}
          ${QuickActions._reportSection('👥 USER STATISTICS', r.userStats)}
          ${QuickActions._reportSection('🔒 SECURITY STATISTICS', r.securityStats)}
        </div>`;
    } catch (e) {
      wrap.innerHTML = `<div class="qa-error">Failed to generate report: ${e.message}</div>`;
    }
  },

  _reportSection(title, obj) {
    const rows = Object.entries(obj || {})
      .map(([k, v]) => `
        <div class="qa-report-row">
          <span class="qa-report-key">${k.replace(/([A-Z])/g, ' $1').toUpperCase()}</span>
          <span class="qa-report-val">${v}</span>
        </div>`).join('');
    return `<div class="qa-report-section"><h4>${title}</h4>${rows}</div>`;
  },

  async _refreshReport() {
    const wrap = document.getElementById('reportContent');
    if (wrap) wrap.innerHTML = `<div class="qa-loading"><span class="loading"></span> Refreshing…</div>`;
    await QuickActions._loadReportData();
  },

  async _downloadCsv() {
    try {
      await AdminAPI.downloadReportCsv();
      UIUtils.showAlert('Report downloaded!', 'success');
    } catch (e) {
      UIUtils.showAlert('Download failed: ' + e.message, 'error');
    }
  },

  // ── Generic close ───────────────────────────────────────────

  closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
  }
};

// ==================== SECURITY SETTINGS MANAGER ====================

const SecurityManager = {

  _2faEnabled: false,
  _setupStep: 0,

  async init() {
    try {
      const status = await AdminAPI.get2FAStatus();
      SecurityManager._2faEnabled = status.enabled;
      SecurityManager._render2FASection(status);
    } catch (e) {
      console.error('Could not load 2FA status:', e);
      const container = document.getElementById('twoFASection');
      if (container) {
        container.innerHTML = `<div class="qa-error" style="font-size:0.8rem;">
          Could not load 2FA status. Ensure you are logged in as admin.</div>`;
      }
    }
  },

  _render2FASection(status) {
    const container = document.getElementById('twoFASection');
    if (!container) return;
    container.innerHTML = status.enabled
      ? SecurityManager._enabledUI(status.maskedPhone)
      : SecurityManager._disabledUI();
  },

  _enabledUI(maskedPhone) {
    return `
      <div class="twofa-status-row">
        <span class="qa-badge qa-badge-green" style="font-size:0.85rem;padding:0.4rem 1rem;">
          ✓ 2FA ACTIVE
        </span>
        <span style="color:var(--text-secondary);font-size:0.85rem;">
          SMS sent to ${maskedPhone || 'your registered phone'}
        </span>
      </div>
      <p style="color:var(--text-muted);font-size:0.82rem;margin:0.75rem 0 1rem;">
        To disable 2FA, confirm your current password below.
      </p>
      <div class="grid grid-2" style="max-width:520px;">
        <div class="form-group">
          <label for="disablePwInput">CURRENT PASSWORD</label>
          <input type="password" id="disablePwInput" placeholder="Confirm password to disable 2FA">
        </div>
      </div>
      <button class="btn btn-secondary"
              style="border-color:#ff3366;color:#ff3366;"
              onclick="SecurityManager.disable2FA()">
        🔓 DISABLE TWO-FACTOR AUTHENTICATION
      </button>`;
  },

  _disabledUI() {
    return `
      <div class="twofa-status-row">
        <span class="qa-badge qa-badge-red" style="font-size:0.85rem;padding:0.4rem 1rem;">
          ✗ 2FA DISABLED
        </span>
      </div>
      <p style="color:var(--text-muted);font-size:0.82rem;margin:0.75rem 0 1rem;">
        Secure your account with a one-time SMS code on every login.
      </p>
      <div id="twoFASetupArea">${SecurityManager._phoneInputUI()}</div>`;
  },

  _phoneInputUI() {
    return `
      <div class="grid grid-2" style="max-width:520px;">
        <div class="form-group">
          <label for="twoFAPhone">PHONE NUMBER (E.164 FORMAT)</label>
          <input type="tel" id="twoFAPhone" placeholder="+91XXXXXXXXXX">
        </div>
      </div>
      <button class="btn btn-secondary" onclick="SecurityManager.initiate2FA()">
        📱 SEND VERIFICATION CODE
      </button>`;
  },

  _otpInputUI() {
    return `
      <p style="color:var(--electric-cyan);font-size:0.85rem;margin-bottom:1rem;">
        ✅ Verification code sent! Enter the 6-digit code from your SMS.
      </p>
      <div class="grid grid-2" style="max-width:520px;">
        <div class="form-group">
          <label for="twoFAOtp">ENTER 6-DIGIT CODE</label>
          <input type="text" id="twoFAOtp" placeholder="123456" maxlength="6"
                 style="letter-spacing:0.3em;font-size:1.2rem;text-align:center;">
        </div>
      </div>
      <div style="display:flex;gap:1rem;flex-wrap:wrap;">
        <button class="btn btn-primary" onclick="SecurityManager.verify2FA()">
          ✓ VERIFY & ENABLE 2FA
        </button>
        <button class="btn btn-secondary" onclick="SecurityManager.resetSetup()">
          ← BACK
        </button>
      </div>`;
  },

  async initiate2FA() {
    const phone = document.getElementById('twoFAPhone')?.value?.trim();
    if (!phone) { UIUtils.showAlert('Please enter a phone number.', 'warning'); return; }
    if (!phone.startsWith('+')) {
      UIUtils.showAlert('Use E.164 format, e.g. +91XXXXXXXXXX', 'warning');
      return;
    }
    const btn = document.querySelector('#twoFASetupArea .btn-secondary');
    const orig = btn ? UIUtils.showLoading(btn) : null;
    try {
      await AdminAPI.initiate2FA(phone);
      const area = document.getElementById('twoFASetupArea');
      if (area) area.innerHTML = SecurityManager._otpInputUI();
      SecurityManager._setupStep = 1;
    } catch (e) {
      UIUtils.showAlert(e.message, 'error');
    } finally {
      if (btn && orig) UIUtils.hideLoading(btn, orig);
    }
  },

  async verify2FA() {
    const otp = document.getElementById('twoFAOtp')?.value?.trim();
    if (!otp || otp.length !== 6) {
      UIUtils.showAlert('Please enter the 6-digit code.', 'warning'); return;
    }
    const btn = document.querySelector('#twoFASetupArea .btn-primary');
    const orig = btn ? UIUtils.showLoading(btn) : null;
    try {
      await AdminAPI.verify2FA(otp);
      UIUtils.showAlert('🎉 Two-factor authentication is now enabled!', 'success');
      SecurityManager._2faEnabled = true;
      SecurityManager._render2FASection({ enabled: true, maskedPhone: '****' });
    } catch (e) {
      UIUtils.showAlert(e.message, 'error');
    } finally {
      if (btn && orig) UIUtils.hideLoading(btn, orig);
    }
  },

  async disable2FA() {
    const pw = document.getElementById('disablePwInput')?.value;
    if (!pw) { UIUtils.showAlert('Enter your current password to disable 2FA.', 'warning'); return; }
    if (!confirm('Are you sure you want to disable two-factor authentication?')) return;
    try {
      await AdminAPI.disable2FA(pw);
      UIUtils.showAlert('Two-factor authentication has been disabled.', 'success');
      SecurityManager._2faEnabled = false;
      SecurityManager._render2FASection({ enabled: false });
    } catch (e) {
      UIUtils.showAlert(e.message, 'error');
    }
  },

  resetSetup() {
    SecurityManager._setupStep = 0;
    const area = document.getElementById('twoFASetupArea');
    if (area) area.innerHTML = SecurityManager._phoneInputUI();
  },

  async changePassword(e) {
    e.preventDefault();
    const current = document.getElementById('currentPassword')?.value;
    const newPw   = document.getElementById('newPassword')?.value;
    const confirm = document.getElementById('confirmPassword')?.value;

    if (newPw !== confirm) {
      UIUtils.showAlert('New passwords do not match!', 'error'); return;
    }
    if (!newPw || newPw.length < 8) {
      UIUtils.showAlert('Password must be at least 8 characters.', 'warning'); return;
    }

    const btn  = document.querySelector('#securityForm button[type="submit"]');
    const orig = btn ? UIUtils.showLoading(btn) : null;
    try {
      await AdminAPI.changePassword(current, newPw);
      UIUtils.showAlert('Password updated successfully!', 'success');
      document.getElementById('securityForm')?.reset();
    } catch (e) {
      UIUtils.showAlert(e.message, 'error');
    } finally {
      if (btn && orig) UIUtils.hideLoading(btn, orig);
    }
  }
};

// Export new globals
window.AdminAPI        = AdminAPI;
window.QuickActions    = QuickActions;
window.SecurityManager = SecurityManager;

// Frontend configuration check
console.log('Smart Energy frontend loaded. API:', API_BASE_URL);


