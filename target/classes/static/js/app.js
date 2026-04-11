document.addEventListener('DOMContentLoaded', () => {
    // Tab Navigation
    const navItems = document.querySelectorAll('.nav-item');
    const views = document.querySelectorAll('.view');
    const viewTitle = document.getElementById('view-title');

    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const target = item.getAttribute('data-target');
            
            // Update nav state
            navItems.forEach(ni => ni.classList.remove('active'));
            item.classList.add('active');

            // Update view state
            views.forEach(v => v.classList.remove('active'));
            document.getElementById(target).classList.add('active');

            // Update title
            viewTitle.textContent = item.querySelector('.text').textContent;
        });
    });

    // --- Manual Analysis Logic (Restored from old app.js with minor tweaks) ---
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const fileInfo = document.getElementById('fileInfo');
    const fileCount = document.getElementById('fileCount');
    const clearBtn = document.getElementById('clearBtn');
    const fileNameDisplay = document.querySelector('.file-name');
    const analyzeBtn = document.getElementById('analyzeBtn');
    const manualLoader = document.getElementById('manual-loader');
    const resultsSection = document.getElementById('resultsSection');
    const tenantIdInput = document.getElementById('tenantId');

    let selectedFiles = [];

    dropZone.addEventListener('click', () => fileInput.click());
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('active');
    });

    ['dragleave', 'drop'].forEach(event => {
        dropZone.addEventListener(event, () => dropZone.classList.remove('active'));
    });

    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        if (e.dataTransfer.files.length) handleFilesSelect(e.dataTransfer.files);
    });

    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length) handleFilesSelect(e.target.files);
    });

    function handleFilesSelect(files) {
        selectedFiles = Array.from(files);
        fileNameDisplay.textContent = selectedFiles.length === 1 ? selectedFiles[0].name : `${selectedFiles.length} Files`;
        fileInfo.classList.remove('hidden');
        dropZone.classList.add('hidden');
    }

    function resetUI() {
        selectedFiles = [];
        fileInput.value = '';
        fileInfo.classList.add('hidden');
        dropZone.classList.remove('hidden');
        resultsSection.classList.add('hidden');
    }

    clearBtn.addEventListener('click', resetUI);

    analyzeBtn.addEventListener('click', async () => {
        if (!selectedFiles.length) return;

        const formData = new FormData();
        selectedFiles.forEach(file => formData.append('files', file));

        manualLoader.classList.remove('hidden');
        fileInfo.classList.add('hidden');
        resultsSection.classList.add('hidden');

        try {
            const response = await fetch('/api/v1/manual/analyze', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) throw new Error('Analysis failed');

            const result = await response.json();
            displayManualResults(result);

        } catch (error) {
            showToast('Error: ' + error.message, 'error');
            fileInfo.classList.remove('hidden');
        } finally {
            manualLoader.classList.add('hidden');
        }
    });

    function displayManualResults(data) {
        resultsSection.innerHTML = `
            <div class="card result-card">
                <div class="result-header">
                    <span class="badge badge-${(data.severity || 'WARN').toLowerCase()}">${data.severity}</span>
                    <h3>Manual Analysis Results</h3>
                </div>
                <div class="result-body">
                    <div style="margin-bottom: 1.5rem">
                        <h4>Summary</h4>
                        <p>${data.logSummary}</p>
                    </div>
                    <div class="solution-preview" style="margin-bottom: 1rem">
                        <strong>AI Solution:</strong><br>${data.suggestedSolution}
                    </div>
                </div>
                <button class="primary-btn" onclick="location.reload()">Process More Logs</button>
            </div>
        `;
        resultsSection.classList.remove('hidden');
    }

    // --- Toast Global ---
    window.showToast = function(message, type = 'success') {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.style.borderBottomColor = type === 'error' ? 'var(--error)' : 'var(--success)';
        toast.classList.remove('hidden');
        setTimeout(() => toast.classList.add('hidden'), 4000);
    };
});
