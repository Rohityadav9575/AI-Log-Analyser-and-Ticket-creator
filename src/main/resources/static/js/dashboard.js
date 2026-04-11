let currentStatus = 'PENDING';

document.addEventListener('DOMContentLoaded', () => {
    const anomalyFeed = document.getElementById('anomaly-feed');
    const totalAnomaliesStat = document.getElementById('stat-total-anomalies');
    const traceModal = document.getElementById('traceModal');
    const closeTraceModal = document.getElementById('closeTraceModal');
    const traceVisual = document.querySelector('.trace-visual');
    const currentTraceIdSpan = document.getElementById('current-trace-id');
    const sidebar = document.getElementById('sidebar');
    const sidebarToggle = document.getElementById('sidebar-toggle');

    // --- Sidebar Toggle Logic ---
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
            localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
        });

        // Restore state
        if (localStorage.getItem('sidebarCollapsed') === 'true') {
            sidebar.classList.add('collapsed');
        }
    }

    // --- Tab Switching Logic ---
    window.switchTab = function(status) {
        currentStatus = status;
        document.querySelectorAll('.status-tab').forEach(btn => btn.classList.remove('active'));
        document.getElementById(`tab-${status.toLowerCase()}`).classList.add('active');
        fetchAnomalies();
    };

    // --- Polling Logic ---
    async function fetchAnomalies() {
        try {
            const response = await fetch(`/api/v1/anomalies?status=${currentStatus}`);
            if (!response.ok) throw new Error('Failed to fetch anomalies');
            
            const anomalies = await response.json();
            renderAnomalies(anomalies);
            updateStats(anomalies.length);
        } catch (error) {
            console.error('Dashboard Error:', error);
        }
    }

    function renderAnomalies(anomalies) {
        anomalyFeed.innerHTML = '';

        if (anomalies.length === 0) {
            anomalyFeed.innerHTML = `<div class="empty-state">No ${currentStatus.toLowerCase()} anomalies found.</div>`;
            return;
        }

        anomalies.forEach(anomaly => {
            const card = document.createElement('div');
            card.className = `anomaly-card priority-${anomaly.severityLevel.toLowerCase()}`;
            
            const timestamp = new Date(anomaly.createdAt).toLocaleString();
            
            card.innerHTML = `
                <div class="anomaly-header">
                    <div>
                        <span class="status-badge status-${anomaly.status.toLowerCase()}">${anomaly.status}</span>
                        <span class="service-tag">${anomaly.serviceName}</span>
                    </div>
                    <span class="timestamp">${timestamp}</span>
                </div>
                <div class="anomaly-content">
                    <strong>[${anomaly.matchedRuleId}]</strong> ${anomaly.logContent}
                </div>
                <div class="anomaly-solution">
                    <strong>AI suggested solution:</strong><br>
                    ${anomaly.suggestedSolution || 'Analysing solution...'}
                </div>
                <div class="anomaly-actions">
                    <button onclick="window.viewTrace('${anomaly.correlationId}')" class="secondary-btn">🔍 View Trace</button>
                    ${anomaly.status === 'PENDING' ? 
                        `<button onclick="window.resolveAnomaly('${anomaly.id}')" class="resolve-btn action-btn">Mark as Resolved</button>` : 
                        `<button onclick="window.reopenAnomaly('${anomaly.id}')" class="reopen-btn action-btn">Re-open Issue</button>`
                    }
                </div>
            `;
            anomalyFeed.appendChild(card);
        });
    }

    window.resolveAnomaly = async function(id) {
        try {
            const response = await fetch(`/api/v1/anomalies/${id}/status?status=RESOLVED`, {
                method: 'PATCH'
            });
            if (response.ok) {
                fetchAnomalies();
                window.showToast('Anomaly marked as resolved!', 'success');
            }
        } catch (error) {
            console.error('Failed to resolve anomaly:', error);
        }
    };

    window.reopenAnomaly = async function(id) {
        try {
            const response = await fetch(`/api/v1/anomalies/${id}/status?status=PENDING`, {
                method: 'PATCH'
            });
            if (response.ok) {
                fetchAnomalies();
                window.showToast('Anomaly re-opened!', 'success');
            }
        } catch (error) {
            console.error('Failed to re-open anomaly:', error);
        }
    };

    window.viewTrace = async function(correlationId) {
        if (!correlationId || correlationId === 'n/a') {
            window.showToast('No execution trace available.', 'error');
            return;
        }

        currentTraceIdSpan.textContent = correlationId;
        traceModal.classList.remove('hidden');
        traceVisual.innerHTML = '<div class="spinner"></div><p style="text-align:center">Fetching trace history...</p>';

        try {
            const response = await fetch(`/api/v1/anomalies/trace/${correlationId}`);
            if (!response.ok) throw new Error('Trace not found');
            
            const trace = await response.json();
            renderTrace(trace);
        } catch (error) {
            traceVisual.innerHTML = `<p class="error-msg">⚠️ Failed to load trace: ${error.message}</p>`;
        }
    };

    function renderTrace(trace) {
        traceVisual.innerHTML = '';
        if (trace.length === 0) {
            traceVisual.innerHTML = '<p class="empty-state">No context trace found for this flow.</p>';
            return;
        }

        trace.forEach(log => {
            const item = document.createElement('div');
            item.className = 'trace-item';
            item.innerHTML = `
                <div class="trace-meta">
                    <strong>${log.serviceName}</strong> | ${log.logLevel} | ${new Date(log.timestamp).toLocaleTimeString()}
                </div>
                <div class="trace-msg">${log.content}</div>
            `;
            traceVisual.appendChild(item);
        });
    }

    function updateStats(count) {
        totalAnomaliesStat.textContent = count;
    }

    closeTraceModal.onclick = () => traceModal.classList.add('hidden');

    // Initialize
    fetchAnomalies();
    setInterval(fetchAnomalies, 5000);
});

// Toast Helper (Assumed available globally or added here)
window.showToast = function(msg, status = 'success') {
    console.log(`TOAST: [${status}] ${msg}`);
};
