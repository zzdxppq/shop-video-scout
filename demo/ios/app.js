/**
 * 拍后合成 - iOS Demo交互逻辑
 * 完整6步创建流程
 */

// App State
const appState = {
    currentView: 'homeView',
    currentStep: 1,
    totalSteps: 6,
    isEditing: false,
    selectedVoice: 'voice-1',
    statusFilter: 'all',
    processingProgress: 0
};

// View navigation
function showView(viewId) {
    // Hide all views
    document.querySelectorAll('.ios-view').forEach(view => {
        view.classList.remove('active');
    });

    // Show target view
    const targetView = document.getElementById(viewId);
    if (targetView) {
        targetView.classList.add('active');
    }

    appState.currentView = viewId;

    // Update tab bar
    updateTabBar(viewId);
}

function updateTabBar(viewId) {
    document.querySelectorAll('.tab-item').forEach(tab => {
        tab.classList.remove('active');
    });

    // Map view to tab
    const viewToTab = {
        'homeView': 0,
        'createStep1': 1,
        'createStep2': 1,
        'createStep3': 1,
        'createStep4': 1,
        'createStep5': 1,
        'createStep6': 1,
        'memberView': 2,
        'historyView': 3,
        'synthesisView': 1,
        'resultView': 3
    };

    const tabIndex = viewToTab[viewId];
    if (tabIndex !== undefined) {
        document.querySelectorAll('.tab-item')[tabIndex]?.classList.add('active');
    }
}

// ===== Step Navigation =====
function goToStep(step) {
    appState.currentStep = step;

    // Map step number to view ID
    const stepViews = {
        1: 'createStep1',
        2: 'createStep2',
        3: 'createStep3',
        4: 'createStep4',
        5: 'createStep5',
        6: 'createStep6'
    };

    showView(stepViews[step]);
    updateStepProgress();
}

function nextStep() {
    if (appState.currentStep < appState.totalSteps) {
        // Special handling for step 2 -> 3 (start AI analysis)
        if (appState.currentStep === 2) {
            showView('createStep3');
            startAIAnalysis();
            return;
        }

        // Special handling for step 5 (start synthesis)
        if (appState.currentStep === 5) {
            startSynthesis();
            return;
        }

        appState.currentStep++;
        goToStep(appState.currentStep);
    }
}

function prevStep() {
    if (appState.currentStep > 1) {
        appState.currentStep--;
        goToStep(appState.currentStep);
    } else {
        showView('homeView');
    }
}

function updateStepProgress() {
    // Update all step progress indicators across all create step views
    const stepViews = ['createStep1', 'createStep2', 'createStep4', 'createStep5'];

    stepViews.forEach(viewId => {
        const view = document.getElementById(viewId);
        if (!view) return;

        const stepItems = view.querySelectorAll('.step-item');
        const progressFill = view.querySelector('.progress-bar-fill');

        stepItems.forEach((item, index) => {
            item.classList.remove('active', 'completed');
            const stepNum = index + 1;

            if (stepNum < appState.currentStep) {
                item.classList.add('completed');
            } else if (stepNum === appState.currentStep) {
                item.classList.add('active');
            }
        });

        // Update progress bar
        if (progressFill) {
            const progress = ((appState.currentStep - 1) / (appState.totalSteps - 1)) * 100;
            progressFill.style.width = progress + '%';
        }
    });
}

// ===== AI Analysis Animation =====
let analysisInterval;

function startAIAnalysis() {
    appState.currentStep = 3;

    const steps = document.querySelectorAll('#createStep3 .analysis-step');
    let currentAnalysisStep = 0;

    // Reset all steps
    steps.forEach(step => {
        step.classList.remove('active', 'completed');
    });

    // Start animation
    analysisInterval = setInterval(() => {
        if (currentAnalysisStep < steps.length) {
            // Mark previous as completed
            if (currentAnalysisStep > 0) {
                steps[currentAnalysisStep - 1].classList.remove('active');
                steps[currentAnalysisStep - 1].classList.add('completed');
            }

            // Mark current as active
            steps[currentAnalysisStep].classList.add('active');
            currentAnalysisStep++;
        } else {
            // All done - mark last as completed
            steps[steps.length - 1].classList.remove('active');
            steps[steps.length - 1].classList.add('completed');

            clearInterval(analysisInterval);

            // Move to step 4 (script generation) after a short delay
            setTimeout(() => {
                appState.currentStep = 4;
                goToStep(4);
            }, 800);
        }
    }, 1200);
}

// ===== Script Editing =====
function toggleScriptEdit() {
    const editBtn = document.querySelector('#createStep4 .edit-btn');
    const scriptViewMode = document.getElementById('scriptViewMode');
    const scriptEditMode = document.getElementById('scriptEditMode');

    if (!appState.isEditing) {
        // Switch to edit mode
        appState.isEditing = true;
        editBtn.textContent = '编辑中';
        editBtn.classList.add('editing');
        scriptViewMode.style.display = 'none';
        scriptEditMode.style.display = 'block';
    } else {
        // Switch back to view mode
        cancelScriptEdit();
    }
}

function saveScriptEdit() {
    const textareas = document.querySelectorAll('#scriptEditMode .segment-textarea');
    const viewTexts = document.querySelectorAll('#scriptViewMode .segment-text');

    // Copy edited text to view mode
    textareas.forEach((textarea, index) => {
        if (viewTexts[index]) {
            viewTexts[index].textContent = textarea.value;
        }
    });

    // Exit edit mode
    cancelScriptEdit();

    // Show feedback
    showToast('脚本已保存');
}

function cancelScriptEdit() {
    const editBtn = document.querySelector('#createStep4 .edit-btn');
    const scriptViewMode = document.getElementById('scriptViewMode');
    const scriptEditMode = document.getElementById('scriptEditMode');

    appState.isEditing = false;
    editBtn.textContent = '编辑';
    editBtn.classList.remove('editing');
    scriptViewMode.style.display = 'block';
    scriptEditMode.style.display = 'none';
}

function regenerateScript() {
    showToast('正在重新生成脚本...');

    // Simulate regeneration
    setTimeout(() => {
        showToast('脚本已更新');
    }, 1500);
}

// ===== Voice Selection =====
function selectVoice(voiceId) {
    appState.selectedVoice = voiceId;

    // Update UI
    document.querySelectorAll('.voice-item').forEach(item => {
        item.classList.remove('selected');
    });

    const selectedItem = document.querySelector(`.voice-item[data-voice="${voiceId}"]`);
    if (selectedItem) {
        selectedItem.classList.add('selected');
    }
}

// ===== Synthesis Process =====
let synthesisInterval;

function startSynthesis() {
    showView('synthesisView');
    appState.processingProgress = 0;

    const percent = document.querySelector('#synthesisView .progress-percent');
    const statusDetail = document.querySelector('#synthesisView .status-detail');
    const steps = document.querySelectorAll('#synthesisView .p-step');

    const stages = [
        '配音生成中...',
        '音频对齐中...',
        '视频渲染中...',
        '导出处理中...'
    ];

    // Reset steps
    steps.forEach(step => {
        step.classList.remove('active', 'completed');
    });

    synthesisInterval = setInterval(() => {
        appState.processingProgress += 1;
        percent.textContent = appState.processingProgress + '%';

        const stageIndex = Math.floor(appState.processingProgress / 25);
        if (stageIndex < stages.length) {
            statusDetail.textContent = stages[stageIndex];
        }

        // Update step indicators
        steps.forEach((step, index) => {
            step.classList.remove('active', 'completed');
            if (index < stageIndex) {
                step.classList.add('completed');
            } else if (index === stageIndex) {
                step.classList.add('active');
            }
        });

        if (appState.processingProgress >= 100) {
            clearInterval(synthesisInterval);
            steps[steps.length - 1].classList.remove('active');
            steps[steps.length - 1].classList.add('completed');

            setTimeout(() => {
                appState.currentStep = 6;
                goToStep(6);
            }, 500);
        }
    }, 50);
}

function returnToHome() {
    // Allow user to return home while synthesis continues
    showView('homeView');
    showToast('视频将在后台继续合成');
}

// ===== History/Task Status =====
function filterHistory(status) {
    appState.statusFilter = status;

    // Update tab UI
    document.querySelectorAll('.status-tab').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelector(`.status-tab[data-status="${status}"]`)?.classList.add('active');

    // Filter items
    const items = document.querySelectorAll('.history-item');
    items.forEach(item => {
        const itemStatus = item.dataset.status;

        if (status === 'all') {
            item.style.display = 'flex';
        } else if (status === 'processing' && (itemStatus === 'processing' || itemStatus === 'queued')) {
            item.style.display = 'flex';
        } else if (status === 'completed' && itemStatus === 'completed') {
            item.style.display = 'flex';
        } else {
            item.style.display = 'none';
        }
    });
}

// ===== Toast Notification =====
function showToast(message) {
    // Remove existing toast
    const existingToast = document.querySelector('.ios-toast');
    if (existingToast) {
        existingToast.remove();
    }

    // Create new toast
    const toast = document.createElement('div');
    toast.className = 'ios-toast';
    toast.innerHTML = `<span>${message}</span>`;
    toast.style.cssText = `
        position: fixed;
        bottom: 120px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(0,0,0,0.8);
        color: white;
        padding: 12px 24px;
        border-radius: 20px;
        font-size: 14px;
        z-index: 1000;
        animation: toastIn 0.3s ease;
    `;

    // Add animation keyframes
    if (!document.querySelector('#toastStyle')) {
        const style = document.createElement('style');
        style.id = 'toastStyle';
        style.textContent = `
            @keyframes toastIn {
                from { opacity: 0; transform: translateX(-50%) translateY(20px); }
                to { opacity: 1; transform: translateX(-50%) translateY(0); }
            }
        `;
        document.head.appendChild(style);
    }

    document.body.appendChild(toast);

    // Auto remove
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 2000);
}

// ===== Device Selector =====
document.querySelectorAll('.device-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.device-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');

        const device = this.dataset.device;
        const frame = document.getElementById('iphoneFrame');

        if (device === 'iphone-se') {
            frame.style.height = '667px';
            frame.style.borderRadius = '40px';
        } else {
            frame.style.height = '812px';
            frame.style.borderRadius = '50px';
        }
    });
});

// ===== Event Delegation Setup =====
document.addEventListener('DOMContentLoaded', () => {
    // Default to home view
    showView('homeView');

    // Add touch feedback to interactive elements
    document.querySelectorAll('.action-card, .work-card, .history-item, .ios-btn-primary, .ios-btn-secondary, .tab-item, .voice-item').forEach(el => {
        el.addEventListener('touchstart', function() {
            this.style.opacity = '0.7';
        });
        el.addEventListener('touchend', function() {
            this.style.opacity = '1';
        });
    });

    // Segment buttons
    document.querySelectorAll('.segment-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            this.parentElement.querySelectorAll('.segment-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // Copy functionality
    document.querySelectorAll('.ios-copy-btn, .copy-single').forEach(btn => {
        btn.addEventListener('click', function() {
            const originalText = this.textContent;
            this.textContent = '已复制';
            setTimeout(() => {
                this.textContent = originalText;
            }, 1500);
        });
    });

    // Tag click to copy
    document.querySelectorAll('.ios-tag').forEach(tag => {
        tag.addEventListener('click', function() {
            this.style.background = 'rgba(0, 122, 255, 0.1)';
            this.style.color = '#007AFF';
            setTimeout(() => {
                this.style.background = '';
                this.style.color = '';
            }, 1000);
        });
    });

    // Plan card selection
    document.querySelectorAll('.plan-card').forEach(card => {
        card.addEventListener('click', function() {
            document.querySelectorAll('.plan-card').forEach(c => {
                c.style.borderColor = 'transparent';
            });
            this.style.borderColor = '#667eea';
        });
    });

    // Voice item selection
    document.querySelectorAll('.voice-item').forEach(item => {
        item.addEventListener('click', function() {
            const voiceId = this.dataset.voice;
            selectVoice(voiceId);
        });
    });

    // Status tabs
    document.querySelectorAll('.status-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            const status = this.dataset.status;
            filterHistory(status);
        });
    });

    // Initialize voice selection
    selectVoice('voice-1');
});

// Handle window visibility change to pause animations
document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        if (analysisInterval) clearInterval(analysisInterval);
        if (synthesisInterval) clearInterval(synthesisInterval);
    }
});
