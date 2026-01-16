/**
 * 拍后合成 - Web版Demo交互逻辑
 */

class ShopVideoScoutDemo {
    constructor() {
        this.currentStep = 1;
        this.totalSteps = 6;
        this.isProcessing = false;
        this.isEditingScript = false;

        this.init();
    }

    init() {
        this.bindEvents();
        this.updateUI();
    }

    bindEvents() {
        // Navigation buttons
        document.getElementById('nextBtn').addEventListener('click', () => this.nextStep());
        document.getElementById('prevBtn').addEventListener('click', () => this.prevStep());

        // Upload area
        const uploadArea = document.getElementById('uploadArea');
        const videoInput = document.getElementById('videoInput');

        uploadArea.addEventListener('click', () => videoInput.click());
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });
        uploadArea.addEventListener('dragleave', () => {
            uploadArea.classList.remove('dragover');
        });
        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            this.handleFileUpload(e.dataTransfer.files);
        });
        videoInput.addEventListener('change', (e) => {
            this.handleFileUpload(e.target.files);
        });

        // Remove video buttons
        document.querySelectorAll('.remove-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.target.closest('.video-item').remove();
                this.updateUploadSummary();
            });
        });

        // Style options
        document.querySelectorAll('.style-option').forEach(option => {
            option.addEventListener('click', () => {
                document.querySelectorAll('.style-option').forEach(o => o.classList.remove('active'));
                option.classList.add('active');
            });
        });

        // Voice cards
        document.querySelectorAll('.voice-card:not(.clone)').forEach(card => {
            card.addEventListener('click', () => {
                document.querySelectorAll('.voice-card').forEach(c => c.classList.remove('active'));
                card.classList.add('active');
            });
        });

        // Voice preview buttons
        document.querySelectorAll('.voice-preview-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.showToast('正在播放音色预览...');
            });
        });

        // Voice upload button
        document.querySelectorAll('.voice-upload-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.showToast('声音克隆功能即将上线');
            });
        });

        // Subtitle style cards
        document.querySelectorAll('.subtitle-style-card').forEach(card => {
            card.addEventListener('click', () => {
                document.querySelectorAll('.subtitle-style-card').forEach(c => c.classList.remove('active'));
                card.classList.add('active');
            });
        });

        // Subtitle toggle
        const subtitleToggle = document.getElementById('subtitleToggle');
        const subtitleStyles = document.getElementById('subtitleStyles');
        if (subtitleToggle && subtitleStyles) {
            subtitleToggle.addEventListener('change', () => {
                subtitleStyles.style.display = subtitleToggle.checked ? 'block' : 'none';
            });
        }

        // Character count for promotion textarea
        const promotionTextarea = document.getElementById('promotion');
        const promotionCount = document.getElementById('promotionCount');
        if (promotionTextarea && promotionCount) {
            promotionTextarea.addEventListener('input', () => {
                promotionCount.textContent = promotionTextarea.value.length;
            });
        }

        // Shop type radio buttons
        document.querySelectorAll('input[name="shopType"]').forEach(radio => {
            radio.addEventListener('change', () => {
                document.querySelectorAll('input[name="shopType"]').forEach(r => {
                    r.closest('.style-option').classList.remove('active');
                });
                radio.closest('.style-option').classList.add('active');
            });
        });

        // Script actions
        document.getElementById('regenerateBtn')?.addEventListener('click', () => {
            this.showToast('正在重新生成脚本...');
            setTimeout(() => this.showToast('脚本已更新'), 1500);
        });

        document.getElementById('editScriptBtn')?.addEventListener('click', () => {
            this.toggleScriptEdit();
        });

        document.getElementById('saveScriptBtn')?.addEventListener('click', () => {
            this.saveScriptEdit();
        });

        document.getElementById('cancelScriptBtn')?.addEventListener('click', () => {
            this.cancelScriptEdit();
        });

        // Start synthesis button (in Step 5)
        document.getElementById('startSynthesisBtn')?.addEventListener('click', () => {
            this.startSynthesis();
        });

        // Slider value updates
        document.querySelectorAll('.slider-container input[type="range"]').forEach(slider => {
            const valueDisplay = slider.nextElementSibling;
            slider.addEventListener('input', () => {
                if (slider.max == 1.2) {
                    valueDisplay.textContent = slider.value + 'x';
                } else {
                    valueDisplay.textContent = slider.value + '%';
                }
            });
        });

        // Copy buttons
        document.querySelectorAll('.copy-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const text = btn.parentElement.querySelector('p').textContent;
                this.copyToClipboard(text);
                this.showToast('已复制到剪贴板');
            });
        });

        document.querySelector('.copy-all-btn')?.addEventListener('click', () => {
            const tags = Array.from(document.querySelectorAll('.tag')).map(t => t.textContent).join(' ');
            this.copyToClipboard(tags);
            this.showToast('已复制全部标签');
        });

        document.querySelectorAll('.tag').forEach(tag => {
            tag.addEventListener('click', () => {
                this.copyToClipboard(tag.textContent);
                this.showToast('已复制: ' + tag.textContent);
            });
        });

        // Download buttons
        document.getElementById('downloadBtn')?.addEventListener('click', () => {
            this.showToast('正在准备下载...');
            setTimeout(() => this.showToast('演示模式：视频下载已模拟完成'), 1500);
        });

        document.getElementById('downloadSourceBtn')?.addEventListener('click', () => {
            this.showToast('演示模式：素材包下载已模拟完成');
        });

        // Create new / View history
        document.getElementById('createNewBtn')?.addEventListener('click', () => {
            this.currentStep = 1;
            this.resetSynthesisState();
            this.updateUI();
            window.scrollTo(0, 0);
        });

        document.getElementById('viewHistoryBtn')?.addEventListener('click', () => {
            this.showToast('历史记录功能即将上线');
        });

        // Back to edit button (from Step 6)
        document.getElementById('backToEditBtn')?.addEventListener('click', () => {
            this.currentStep = 4;
            this.resetSynthesisState();
            this.updateUI();
            this.showToast('已返回脚本编辑页');
        });

        // Play button
        document.querySelector('.play-button-large')?.addEventListener('click', () => {
            this.showToast('演示模式：视频预览');
        });

        // Help button
        document.getElementById('helpBtn')?.addEventListener('click', () => {
            this.showToast('帮助中心即将上线');
        });
    }

    nextStep() {
        if (this.isProcessing) return;

        // Validate current step
        if (!this.validateStep(this.currentStep)) return;

        // Special handling for Step 2 -> Step 3 (AI Analysis)
        if (this.currentStep === 2) {
            this.startAIAnalysis();
            return;
        }

        // Normal step progression
        if (this.currentStep < this.totalSteps) {
            this.currentStep++;
            this.updateUI();
        }
    }

    prevStep() {
        if (this.isProcessing) return;

        if (this.currentStep > 1) {
            this.currentStep--;
            // Reset synthesis state when going back from step 5
            if (this.currentStep === 4) {
                this.resetSynthesisState();
            }
            this.updateUI();
        }
    }

    validateStep(step) {
        switch (step) {
            case 1:
                const shopName = document.getElementById('shopName').value.trim();
                const shopTypeSelected = document.querySelector('input[name="shopType"]:checked');
                if (!shopName) {
                    this.showToast('请填写店铺名称');
                    return false;
                }
                if (!shopTypeSelected) {
                    this.showToast('请选择店铺类型');
                    return false;
                }
                return true;
            case 2:
                const videoCount = document.querySelectorAll('.video-item').length;
                if (videoCount < 1) {
                    this.showToast('请至少上传1个视频');
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    startAIAnalysis() {
        this.isProcessing = true;
        this.currentStep = 3;
        this.updateUI();

        const processingState = document.getElementById('processingState');
        const analysisResults = document.getElementById('analysisResults');

        processingState.style.display = 'block';
        analysisResults.style.display = 'none';

        // Simulate AI analysis
        let progress = 0;
        const progressDetail = document.querySelector('.processing-detail');
        const progressText = document.querySelector('.processing-progress span');

        const stages = [
            '正在识别镜头内容...',
            '正在分析视频质量...',
            '正在评估镜头构图...',
            '正在匹配最佳片段...',
            '正在生成推荐方案...'
        ];

        const interval = setInterval(() => {
            progress++;
            const stageIndex = Math.min(Math.floor(progress / 20), stages.length - 1);
            progressDetail.textContent = stages[stageIndex];
            progressText.textContent = `分析中 ${Math.min(progress * 6 / 100 + 1, 6).toFixed(0)}/6`;

            if (progress >= 100) {
                clearInterval(interval);
                setTimeout(() => {
                    processingState.style.display = 'none';
                    analysisResults.style.display = 'block';
                    this.isProcessing = false;
                    this.updateNavButtons();
                }, 500);
            }
        }, 50);
    }

    startSynthesis() {
        this.isProcessing = true;

        const synthesisOptions = document.getElementById('synthesisOptions');
        const synthesisProgress = document.getElementById('synthesisProgress');

        // Hide options, show progress
        synthesisOptions.style.display = 'none';
        synthesisProgress.style.display = 'block';

        this.updateNavButtons();

        // Simulate synthesis progress
        const steps = document.querySelectorAll('#synthesisProgress .progress-step');
        const progressFill = document.querySelector('#synthesisProgress .progress-fill');
        const progressText = document.querySelector('#synthesisProgress .progress-text');

        let currentProgressStep = 0;
        const progressValues = [25, 50, 75, 100];

        const advanceStep = () => {
            if (currentProgressStep < steps.length) {
                // Complete previous step
                if (currentProgressStep > 0) {
                    steps[currentProgressStep - 1].classList.remove('active');
                    steps[currentProgressStep - 1].classList.add('completed');
                    steps[currentProgressStep - 1].querySelector('.step-icon').innerHTML = '✓';
                }

                // Activate current step
                steps[currentProgressStep].classList.remove('completed');
                steps[currentProgressStep].classList.add('active');
                steps[currentProgressStep].querySelector('.step-icon').innerHTML = '<div class="spinner-mini"></div>';

                progressFill.style.width = progressValues[currentProgressStep] + '%';
                progressText.textContent = `合成进度: ${progressValues[currentProgressStep]}%`;

                currentProgressStep++;
                setTimeout(advanceStep, 1200);
            } else {
                // Complete last step
                if (steps.length > 0) {
                    steps[steps.length - 1].classList.remove('active');
                    steps[steps.length - 1].classList.add('completed');
                    steps[steps.length - 1].querySelector('.step-icon').innerHTML = '✓';
                }
                progressFill.style.width = '100%';
                progressText.textContent = '合成进度: 100%';

                // Navigate to Step 6
                setTimeout(() => {
                    this.isProcessing = false;
                    this.currentStep = 6;
                    this.updateUI();
                    this.showToast('视频合成完成！');
                }, 800);
            }
        };

        setTimeout(advanceStep, 800);
    }

    resetSynthesisState() {
        const synthesisOptions = document.getElementById('synthesisOptions');
        const synthesisProgress = document.getElementById('synthesisProgress');

        if (synthesisOptions) synthesisOptions.style.display = 'block';
        if (synthesisProgress) synthesisProgress.style.display = 'none';

        // Reset progress steps
        const steps = document.querySelectorAll('#synthesisProgress .progress-step');
        steps.forEach((step, index) => {
            step.classList.remove('active', 'completed');
            step.querySelector('.step-icon').innerHTML = index + 1;
        });

        const progressFill = document.querySelector('#synthesisProgress .progress-fill');
        const progressText = document.querySelector('#synthesisProgress .progress-text');
        if (progressFill) progressFill.style.width = '0%';
        if (progressText) progressText.textContent = '合成进度: 0%';
    }

    toggleScriptEdit() {
        const scriptContent = document.getElementById('scriptContent');
        const scriptEditMode = document.getElementById('scriptEditMode');
        const editBtn = document.getElementById('editScriptBtn');

        if (!this.isEditingScript) {
            // Enter edit mode
            scriptContent.style.display = 'none';
            scriptEditMode.style.display = 'block';
            editBtn.style.display = 'none';
            this.isEditingScript = true;
            this.showToast('已进入编辑模式，可修改脚本内容');
        }
    }

    saveScriptEdit() {
        const scriptContent = document.getElementById('scriptContent');
        const scriptEditMode = document.getElementById('scriptEditMode');
        const editBtn = document.getElementById('editScriptBtn');

        // Update script content from textareas
        const textareas = scriptEditMode.querySelectorAll('textarea');
        const segments = scriptContent.querySelectorAll('.segment-text');

        textareas.forEach((textarea, index) => {
            if (segments[index]) {
                segments[index].textContent = textarea.value;
            }
        });

        // Exit edit mode
        scriptContent.style.display = 'block';
        scriptEditMode.style.display = 'none';
        editBtn.style.display = 'flex';
        this.isEditingScript = false;

        this.updateScriptStats();
        this.showToast('脚本已保存');
    }

    cancelScriptEdit() {
        const scriptContent = document.getElementById('scriptContent');
        const scriptEditMode = document.getElementById('scriptEditMode');
        const editBtn = document.getElementById('editScriptBtn');

        // Restore original content to textareas
        const textareas = scriptEditMode.querySelectorAll('textarea');
        const segments = scriptContent.querySelectorAll('.segment-text');

        textareas.forEach((textarea, index) => {
            if (segments[index]) {
                textarea.value = segments[index].textContent;
            }
        });

        // Exit edit mode
        scriptContent.style.display = 'block';
        scriptEditMode.style.display = 'none';
        editBtn.style.display = 'flex';
        this.isEditingScript = false;

        this.showToast('已取消编辑');
    }

    updateScriptStats() {
        const segments = document.querySelectorAll('#scriptContent .segment-text');
        let totalChars = 0;
        segments.forEach(seg => {
            totalChars += seg.textContent.length;
        });

        // Update stats display
        const charsStat = document.querySelector('.script-stats .stat-value:nth-child(2)');
        if (charsStat) {
            // Find the chars stat
            document.querySelectorAll('.script-stats .stat-item').forEach(item => {
                if (item.querySelector('.stat-label').textContent === '脚本字数') {
                    item.querySelector('.stat-value').textContent = totalChars + '字';
                }
            });
        }

        // Estimate duration (roughly 4 chars per second for Chinese)
        const estimatedSeconds = Math.round(totalChars / 4);
        document.querySelectorAll('.script-stats .stat-item').forEach(item => {
            if (item.querySelector('.stat-label').textContent === '预计时长') {
                item.querySelector('.stat-value').textContent = estimatedSeconds + '秒';
            }
        });
    }

    updateUI() {
        // Update step indicators
        document.querySelectorAll('.progress-steps .step').forEach((step, index) => {
            const stepNum = index + 1;
            step.classList.remove('active', 'completed');

            if (stepNum < this.currentStep) {
                step.classList.add('completed');
            } else if (stepNum === this.currentStep) {
                step.classList.add('active');
            }
        });

        // Update progress bar
        const progress = ((this.currentStep - 1) / (this.totalSteps - 1)) * 100;
        document.querySelector('.progress-fill').style.width = `${Math.max(progress, 8.33)}%`;

        // Update step content
        document.querySelectorAll('.step-content').forEach((content, index) => {
            content.classList.remove('active');
            if (index + 1 === this.currentStep) {
                content.classList.add('active');
            }
        });

        // Update navigation buttons
        this.updateNavButtons();

        // Scroll to top of content
        document.querySelector('.main-content').scrollTop = 0;
    }

    updateNavButtons() {
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');

        prevBtn.disabled = this.currentStep === 1 || this.isProcessing;

        if (this.currentStep === this.totalSteps) {
            nextBtn.style.display = 'none';
        } else if (this.currentStep === 5) {
            // On Step 5, hide the main next button (use the synthesis button instead)
            nextBtn.style.display = 'none';
        } else {
            nextBtn.style.display = 'flex';
            nextBtn.disabled = this.isProcessing;

            if (this.currentStep === 2) {
                nextBtn.innerHTML = `开始分析 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>`;
            } else {
                nextBtn.innerHTML = `下一步 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>`;
            }
        }
    }

    handleFileUpload(files) {
        this.showToast(`已选择 ${files.length} 个视频文件`);
        // In a real app, we would add the files to the list
    }

    updateUploadSummary() {
        const count = document.querySelectorAll('.video-item').length;
        const summary = document.querySelector('.upload-summary');
        if (summary) {
            summary.querySelector('strong').textContent = count;
        }
    }

    copyToClipboard(text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text);
        } else {
            // Fallback
            const textarea = document.createElement('textarea');
            textarea.value = text;
            document.body.appendChild(textarea);
            textarea.select();
            document.execCommand('copy');
            document.body.removeChild(textarea);
        }
    }

    showToast(message) {
        const toast = document.getElementById('toast');
        toast.querySelector('.toast-message').textContent = message;
        toast.classList.add('show');

        setTimeout(() => {
            toast.classList.remove('show');
        }, 2500);
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    new ShopVideoScoutDemo();
});
