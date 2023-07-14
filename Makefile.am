# Copyright (C) 2022 Free Software Foundation
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.

AUTOMAKE_OPTIONS = foreign no-dist
ACLOCAL_AMFLAGS = -I . 

VPATH =
SRC     = $(srcdir)
JAVA    = java
JAVAC   = javac
JAVA_FLAGS =
JAVAC_FLAGS =
JAR = jar

JAVA_ANMODULES_DIR	= org/gprofng
JAVA_ANMODULE_DIR	= $(JAVA_ANMODULES_DIR)/mpmt
JAVA_ANALYZER_DIR	= $(JAVA_ANMODULES_DIR)/analyzer
JAVA_PROFAPI_DIR	= $(JAVA_ANMODULES_DIR)/collector

JAR_FLAGS = cfvm

AN_SRC  = \
	AboutPanel.java \
	AnAction.java \
	AnChooser.java \
	AnCompDisp.java \
	AnDialog.java \
	AnDisplay.java \
	AnDouble.java \
	AnEvent.java \
	AnFile.java \
	AnFileSystemView.java \
	AnFrame.java \
	AnHotGap.java \
	AnIconButton.java \
	AnInteger.java \
	AnList.java \
	AnListener.java \
	AnLocale.java \
	AnLong.java \
	AnMemoryManager.java \
	AnMetric.java \
	AnObject.java \
	AnShellCommand.java \
	AnString.java \
	AnTable.java \
	AnTree.java \
	AnVariable.java \
	AnWindow.java \
	Analyzer.java \
	CallTreeView.java \
	CalledByCallsDisp.java \
	CalledByCallsDisassemblyView.java \
	CalledByCallsFunctionsView.java \
	CalledByCallsSourceView.java \
	CallerCalleesView.java \
	CannotFindFilePanel.java \
	ConfigurationPanel.java \
	DataLayoutView.java \
	DataObjectsView.java \
	DisasmDisp.java \
	DisassemblyView.java \
	DualSourceView.java \
	ExperimentPickList.java \
	ExperimentPickListElement.java \
	ExperimentPickLists.java \
	ExperimentsView.java \
	FuncListDisp.java \
	FunctionsView.java \
	IDispToModel.java \
	IExtTreeModel.java \
	IOView.java \
	HeapView.java \
	HotGapPanel.java \
	IPC.java \
	IPCProcess.java \
	ITreeToPanel.java \
	InstructionFrequencyView.java \
	KeyboardShortcuts.java \
	LibraryVisibilityPanel.java \
	LibraryVisibilityDialog.java \
	LinesView.java \
	MemoryIndexObjectView.java \
	NoMetricsSelectedPanel.java \
	MiniFuncListDisp.java \
	PCsView.java \
	PsParser.java \
	ResolveFilePathDialog.java \
	ResolveFilePathPanel.java \
	SelectedObject.java \
	SelectionManager.java \
	SourceDisassemblyView.java \
	SourceDisp.java \
	SourceView.java \
	StatisticsView.java \
	SummaryPanel.java \
	ViewDisplayPanel.java \
	control_panel/CPDetailsScrollPane.java \
	control_panel/CPFilterPanelHandler.java \
	control_panel/CPLayoutPanel.java \
	control_panel/CPToolbar.java \
	control_panel/CPToolbarHandler.java \
	experiment_props/DataDescriptor.java \
	experiment_props/ExperimentProperties.java \
	experiment_props/Experiments.java \
	experiment_props/PropDescriptor.java \
	util/ruler/LabelCoordinateCalculator.java \
	util/ruler/RangeRuler.java \
	util/ruler/Ruler.java \
	util/ruler/valuetypes/ValuesBytes.java \
	util/ruler/valuetypes/ValuesGeneric.java \
	util/ruler/valuetypes/ValuesLabelList.java \
	util/ruler/valuetypes/ValuesLong.java \
	util/ruler/valuetypes/ValuesNanoseconds.java \
	util/ruler/valuetypes/ValuesWithUnits.java \
	remote/Authentication.java \
	remote/AuthenticationPanel.java \
	remote/CheckBoxList.java \
	remote/CloseExperimentDialog.java \
	remote/CloseExperimentPanel.java \
	remote/ConnectionDialog.java \
	remote/ConnectionManager.java \
	remote/ConnectionPanel.java \
	timeline2/Entity.java \
	timeline2/EventDetails.java \
	timeline2/IconRuler.java \
	timeline2/MetaExperiment.java \
	timeline2/RowData.java \
	timeline2/RowDataRequestParams.java \
	timeline2/RowDefinition.java \
	timeline2/TL2ControlPanel.java \
	timeline2/TL2CoordCalcData.java \
	timeline2/TL2DataFetcher.java \
	timeline2/TL2DataSnapshot.java \
	timeline2/TL2Drawer.java \
	timeline2/TLExperiment.java \
	timeline2/TimelineDraw.java \
	timeline2/TimelineSelectionEvent.java \
	timeline2/TimelineSelectionRowEvent.java \
	timeline2/TimelineVariable.java \
	timeline2/TimelineView.java \
	flame/FlameBlock.java \
	flame/FlameData.java \
	flame/FlamePanel.java \
	flame/FlameRow.java \
	flame/FlameView.java \
	flame/FlameToolBar.java \
	timeline2/data/GenericEvent.java \
	timeline2/data/SampleEvent.java \
	timeline2/data/StackEvent.java \
	timeline2/cursorevent/CursorEvent.java \
	timeline2/cursorevent/CursorMoveEvent.java \
	timeline2/cursorevent/CursorSetEvent.java \
	timeline_common/CoordCalcDataImpl.java \
	timeline_common/CoordCalcTimeImpl.java \
	timeline_common/CoordCalcDataMaster.java \
	timeline_common/CoordCalcTimeMaster.java \
	timeline_common/CoordCalcDataReader.java \
	timeline_common/CoordCalcTimeReader.java \
	timeline_common/TimelineCaliper.java \
	timeline_common/TimelineCursor.java \
	timeline_common/TimelineDrawer.java \
	timeline_common/TimelineSelectionCaliperEvent.java \
	timeline_common/TimelineSelectionGenericEvent.java \
	timeline_common/TimelineSelectionHistoryEvent.java \
	timeline_common/TimelineSelectionListener.java \
	timeline_common/TimelinePanel.java \
	timeline_common/VerticalRowRuler.java \
	timeline/events/DeadlockEvent.java \
	timeline/events/DetailsIPC.java \
	timeline/events/DurationEvent.java \
	timeline/events/EventDetail.java \
	timeline/events/ExtendedEvent.java \
	timeline/events/HeapEvent.java \
	timeline/events/HWCEvent.java \
	timeline/events/IOEvent.java \
	timeline/events/MPIEvent.java \
	timeline/events/ProfileEvent.java \
	timeline/events/RaceEvent.java \
	timeline/events/Sample.java \
	timeline/events/SyncEvent.java \
	timeline/events/StateEvent.java \
	statecolors/AnColorChooser.java \
	statecolors/ChooserStateView.java \
	statecolors/ColorMap.java \
	statecolors/ColorRule.java \
	statecolors/StackState.java \
	statecolors/StackStateList.java \
	statecolors/StackView.java \
	statecolors/StackViewState.java \
	statecolors/StateColorMap.java \
	util/zoomruler/OverlayMouseHandler.java \
	util/zoomruler/PaintListener.java \
	util/zoomruler/ZoomRulerComponent.java \
	util/zoomruler/ZoomRulerEvent.java \
	util/zoomruler/ZoomRulerListener.java \
	util/zoomruler/ZoomRulerOverlay.java \
	util/gui/ActionTextField.java \
	util/gui/AnBorderPanel.java \
	util/gui/AnCheckBox.java \
	util/gui/AnDialog2.java \
	util/gui/AnDialogOuterPanel.java \
	util/gui/AnGradientPanel.java \
	util/gui/AnInsetsPanel.java \
	util/gui/AnJList.java \
	util/gui/AnLongScrollBar.java \
	util/gui/AnJPanel.java \
	util/gui/AnJScrollPane.java \
	util/gui/AnJTree.java \
	util/gui/AnMenuBar.java \
	util/gui/AnScrollBarUI.java \
	util/gui/AnSplitPane.java \
	util/gui/AnSplitPaneFixedRightSize.java \
	util/gui/AnSplitPaneInternal.java \
	util/gui/AnTabbedPane.java \
	util/gui/AnTabbedPaneUI.java \
	util/gui/AnTextIcon.java \
	util/gui/AnUtility.java \
	util/gui/RainbowColorScale.java \
	util/gui/ToolTipPopup.java \
	progress/CancelContext.java \
	progress/Cancellable.java \
	progress/InternalHandle.java \
	progress/ProgressHandle.java \
	progress/ProgressPanel.java \
	progress/SystemProgressPanel.java \
	navigation/NavigationPanel.java \
	navigation/PanelDropTargetListener.java \
	navigation/TopNavigationPanel.java \
	navigation/View.java \
	navigation/ViewPanel.java \
	navigation/ViewsPanel.java \
	welcome/WelcomeButton.java \
	welcome/WelcomeInnerPanel.java \
	welcome/WelcomePanel.java \
	welcome/WelcomeView.java \
	collect/CollectDialog.java \
	collect/CollectPanel.java \
	collect/CollectPanel2.java \
	collect/CollectPanel3.java \
	collect/CollectUtility.java \
	collect/Collector.java \
	collect/CollectorOpenPanel.java \
	collect/HWCEntry.java \
	collect/HWCSelectDialog.java \
	collect/HWCSelectPanel.java \
	persistence/UserPref.java \
	persistence/UserPrefPersistence.java \
	event/AnChangeEvent.java \
	event/AnChangeListener.java \
	event/AnEventManager.java \
	event/AnSelectionEvent.java \
	settings/AnSettingChangeEvent.java \
	settings/CallTreePanel.java \
	settings/CallTreeSetting.java \
	settings/CompareModeSetting.java \
	settings/ExportSettingsDialog.java \
	settings/ExportSettingsPanel.java \
	settings/FormatSetting.java \
	settings/ImportSettingsDialog.java \
	settings/ImportSettingsPanel.java \
	settings/LibraryVisibilitySetting.java \
	settings/MMetric.java \
	settings/MetricAttributes.java \
	settings/MetricSelection.java \
	settings/MetricNameSelection.java \
	settings/MetricNameSelected.java \
	settings/MetricState.java \
	settings/MetricStates.java \
	settings/MetricType.java \
	settings/MetricsPanel.java \
	settings/MetricsSetting.java \
	settings/PathMapSetting.java \
	settings/PathmapsPanel.java \
	settings/SearchPathPanel.java \
	settings/SearchPathSetting.java \
	settings/Setting.java \
	settings/Settings.java \
	settings/SourceDisassemblySetting.java \
	settings/TableSettings.java \
	settings/TimelineSetting.java \
	settings/ViewModeEnabledSetting.java \
	settings/ViewModeSetting.java \
	settings/ViewsSetting.java \
	guitesting/GUITesting.java \
	mainview/BorderPanel.java \
	mainview/MainViewPanel.java \
	mainview/Panel.java \
	mainview/Subview.java \
	mainview/SubviewAreaPanel.java \
	table/AnTableHeaderPanel.java \
	table/AnTableScrollPane.java \
	table/AttributePanel.java \
	table/ExclusiveAndInclusivePanel.java \
	table/ExclusiveOrInclusivePanel.java \
	table/HeaderMouseHandler.java \
	table/MetricPanel.java \
	table/MetricPanelDropTargetListener.java \
	overview/AnBarTree.java \
	overview/Bar.java \
	overview/BarPanel.java \
	overview/CheckBoxBarPanel.java \
	overview/GradientPanel.java \
	overview/OuterPanel.java \
	overview/Overview.java \
	overview/OverviewView.java \
	overview/OverviewPanel.java \
	overview/ProgressBarPanel.java \
	overview/TopButtonPanel.java \
	overview/TurnerLabelPanel.java \
	overview/TurnerPanel.java \
	export/Export.java \
	export/ExportDialog.java \
	export/ExportPanel.java \
	export/ExportSupport.java \
	toolbar/FindTextPanel.java \
	toolbar/ToolBarFiller.java \
	toolbar/ToolBarPanel.java \
	toolbar/ToolBarSeparator.java \
	filter/CustomEditPanel.java \
	filter/CustomFilterAction.java \
	filter/CustomFilterDialog.java \
	filter/CustomPanel.java \
	filter/FilterClause.java \
	filter/FilterParser.java \
	filter/Filters.java \
	filter/FilterNavigationPanel.java \
	filter/FilterStatusPanel.java \
	filter/FilterTextField.java \
	filter/KeywordsPanel.java \
	filter/RedoFilterAction.java \
	filter/RemoveAllFilterAction.java \
	filter/UndoFilterAction.java \
	compare/AnTransferHandler.java \
	compare/CompareAdvancedDialog.java \
	compare/CompareGroupsPanel.java \
	compare/CompareNavigationPanel.java \
	compare/CompareSimpleDialog.java \
	compare/CompareSimplePanel.java \
	compare/CompareStatusPanel.java \
	compare/ExperimentGroupsPanel.java \
	picklist/PickList.java \
	picklist/PickListElement.java \
	picklist/PickListEvent.java \
	picklist/PickListListener.java \
	picklist/StringPickList.java \
	picklist/StringPickListElement.java \
	statuspanel/StatusComponent.java \
	statuspanel/StatusComponentHandle.java \
	statuspanel/StatusHandleFactory.java \
	statuspanel/StatusLabel.java \
	statuspanel/StatusLabelHandle.java \
	statuspanel/StatusLabelValueHandle.java \
	statuspanel/StatusPanel.java \
	ipc/IPCCancelledException.java \
	ipc/IPCContext.java \
	ipc/IPCErrorReader.java \
	ipc/IPCHandle.java \
	ipc/IPCListener.java \
	ipc/IPCLogger.java \
	ipc/IPCProtocol.java \
	ipc/IPCReader.java \
	ipc/IPCRequest.java \
	ipc/IPCRequestHeader.java \
	ipc/IPCResponse.java \
	ipc/IPCResponseHeader.java \
	ipc/IPCResult.java \
	metrics/BasicMetric.java \
	metrics/CompareBarData.java \
	metrics/IPCMetricsAPI.java \
	metrics/MetricAttr.java \
	metrics/MetricNode.java \
	metrics/MetricOption.java \
	metrics/MetricColors.java \
	metrics/MetricLabel.java \
	metrics/MetricsGUI.java \
	metrics/MetricValue.java \
	metrics/SelectableMetricNode.java \
	metrics/ValueMetricNode.java \
	$(NULL)

anmodule_FILES  = \
	$(AN_SRC:%.java=$(srcdir)/$(JAVA_ANMODULE_DIR)/%.java) \
	$(ANALYZER_FILES)

anmodule_ICONS	= \
	add.png \
	apply_mtrs.png \
	apply_filter.png \
	analyzer64.png \
	attributed.png \
	backward.png \
	back_filter.png \
	badexperiment.png \
	blank.png \
	bubble_icon_16.png \
	callee.png \
	caller.png \
	cancelProcess.png \
	circleBlue.gif \
	circleGreen.gif \
	circleOrange.gif \
	circleRed.gif \
	class.png \
	collect.png \
	color.png \
	number.png \
	percent.png \
	time.png \
	compare.png \
	compareAbsolute.png \
	compareDelta.png \
	compareHamburger.png \
	compareRatio.png \
	compareReverse.png \
	connect.png \
	data.png \
	i_o_usage_16.png \
	delete.png \
	down.png \
	exclusive.png \
	exe_elf.png \
	expandcollaps.png \
	experiment.png \
	expgroup.png \
	green100.png \
	filter.png \
	fileResolved.png \
	fileUnResolved.png \
	find_next.png \
	find_prev.png \
	forward.png \
	forward_filter.png \
	func_item.png \
	gear.png \
	hamburger.png \
	hamburgerBlank.png \
	hamburgerFocused.png \
	gobackward.png \
	godown.png \
	goforward.png \
	goup.png \
	heaptrace.png \
	hollowArrowDown.png \
	hollowArrowLeft.png \
	hollowArrowRight.png \
	hollowArrowUp.png \
	hwc.png \
	hot.png \
	inclusive.png \
	jar.png \
	more_icon.png \
	mpitrace.png \
	nav_bar_arrow.png \
	nextview.png \
	oldexperiment.png \
	open.png \
	panhand.png \
	addtablecolumns.png \
	profile.png \
	previousview.png \
	small_down.png \
	small_left.png \
	small_right.png \
	small_up.png \
	smallBlank.png \
	small_down_focused.png \
	small_up_focused.png \
	races.png \
	remove.png \
	removeBlank.png \
	removeFocused.png \
	redo.png \
	reset.png \
	sample.png \
	save.png \
	performanceAnalyzerSplash.gif \
	stop.png \
	synctrace.png \
	tab.png \
	values.png \
	undo.png \
	up.png \
	base1.png \
	base2.png \
	up.png \
	warnexperiment.png \
	warning.png \
	error.png \
	warningNew.png \
	errorNew.png \
	errorWarningNew.png \
	welcomeBackgroundShadow.png \
	zoom_in.png \
	zoom_out.png \
	$(NULL)

GPROFNG_CLASSES	= \
	$(JAVA_ANMODULE_DIR)/*.class \
	$(JAVA_ANMODULE_DIR)/control_panel/*.class \
	$(JAVA_ANMODULE_DIR)/progress/*.class \
	$(JAVA_ANMODULE_DIR)/remote/*.class \
	$(JAVA_ANMODULE_DIR)/navigation/*.class \
	$(JAVA_ANMODULE_DIR)/welcome/*.class \
	$(JAVA_ANMODULE_DIR)/collect/*.class \
	$(JAVA_ANMODULE_DIR)/persistence/*.class \
	$(JAVA_ANMODULE_DIR)/event/*.class \
	$(JAVA_ANMODULE_DIR)/settings/*.class \
	$(JAVA_ANMODULE_DIR)/experiment_props/*.class \
	$(JAVA_ANMODULE_DIR)/guitesting/*.class \
	$(JAVA_ANMODULE_DIR)/mainview/*.class \
	$(JAVA_ANMODULE_DIR)/table/*.class \
	$(JAVA_ANMODULE_DIR)/overview/*.class \
	$(JAVA_ANMODULE_DIR)/export/*.class \
	$(JAVA_ANMODULE_DIR)/toolbar/*.class \
	$(JAVA_ANMODULE_DIR)/filter/*.class \
	$(JAVA_ANMODULE_DIR)/compare/*.class \
	$(JAVA_ANMODULE_DIR)/picklist/*.class \
	$(JAVA_ANMODULE_DIR)/statuspanel/*.class \
	$(JAVA_ANMODULE_DIR)/ipc/*.class \
	$(JAVA_ANMODULE_DIR)/metrics/*.class \
	$(JAVA_ANMODULE_DIR)/statecolors/*.class \
	$(JAVA_ANMODULE_DIR)/timeline/events/*.class \
	$(JAVA_ANMODULE_DIR)/timeline2/*.class \
	$(JAVA_ANMODULE_DIR)/timeline2/cursorevent/*.class \
	$(JAVA_ANMODULE_DIR)/timeline2/data/*.class \
	$(JAVA_ANMODULE_DIR)/flame/*.class \
	$(JAVA_ANMODULE_DIR)/timeline_common/*.class \
	$(JAVA_ANMODULE_DIR)/util/ruler/*.class \
	$(JAVA_ANMODULE_DIR)/util/ruler/valuetypes/*.class \
	$(JAVA_ANMODULE_DIR)/util/zoomruler/*.class \
	$(JAVA_ANMODULE_DIR)/util/gui/*.class \
	$(NULL)

anmodule_MF		= $(srcdir)/gprofng-gui.mf

anmodule_PROPERTIES = $(JAVA_ANMODULE_DIR)/Bundle.properties

$(JAVA_ANMODULE_DIR) $(JAVA_ANALYZER_DIR) $(JAVA_PROFAPI_DIR) $(JAVA_ANMODULES_DIR):
	@echo "================= $@"
	mkdir -m 777 -p $@

LocaleString.class: $(srcdir)/tools/LocaleString.java
	@echo "================= $@"
	time $(JAVAC) $(JAVAC_FLAGS) -d . $(srcdir)/tools/LocaleString.java

$(anmodule_PROPERTIES): $(JAVA_ANMODULE_DIR) $(anmodule_FILES) LocaleString.class \
		      $(srcdir)/gprofng.properties
	@echo "================= $@"
	echo "# THIS FILE IS AUTO-GENERATED. DON'T EDIT IT." > $@
	echo "" >> $@
	cat $(srcdir)/gprofng.properties >> $@
	cat $(anmodule_FILES) | $(JAVA) $(JAVA_FLAGS) -cp . LocaleString >> $@

anmodule_RESOURCE = \
	$(anmodule_ICONS:%=$(JAVA_ANMODULE_DIR)/icons/%) \
	$(NULL)

anmodule_JAR = gprofng.jar

$(anmodule_JAR): $(JAVA_ANMODULE_DIR) $(anmodule_MF) $(anmodule_FILES) \
		 $(anmodule_PROPERTIES) $(anmodule_RESOURCE:%=$(srcdir)/%)
	@echo "================= $@"
	time $(JAVAC) -cp $(srcdir) $(JAVAC_FLAGS) -d . $(srcdir)/$(JAVA_ANMODULE_DIR)/Analyzer.java
	$(JAR) $(JAR_FLAGS) $@ $(anmodule_MF) $(GPROFNG_CLASSES) \
		$(anmodule_RESOURCE:%=-C $(srcdir) %) $(anmodule_PROPERTIES)

#
# Standalone gprofng GUI
#
ANALYZER_SRC    = AnEnvironment.java AnLog.java AnMain.java
ANALYZER_FILES  = $(ANALYZER_SRC:%.java=$(srcdir)/$(JAVA_ANALYZER_DIR)/%.java)
ANALYZER_MF     = $(srcdir)/analyzer_st.mf

ANALYZER_CLASSES = $(JAVA_ANALYZER_DIR)/*.class


ANALYZER_PROPERTIES	= $(JAVA_ANALYZER_DIR)/Bundle.properties

$(ANALYZER_PROPERTIES): $(JAVA_ANALYZER_DIR) $(ANALYZER_FILES) LocaleString.class
	@echo "================= $@"
	echo "# THIS FILE IS AUTO-GENERATED. DON'T EDIT IT." > $@;
	echo "" >> $@
	cat $(ANALYZER_FILES) | $(JAVA) $(JAVA_FLAGS) -cp . LocaleString >> $@

ANALYZER_JAR = gprofng-analyzer.jar

$(ANALYZER_JAR): $(JAVA_ANALYZER_DIR) $(anmodule_JAR) \
		    $(ANALYZER_MF) $(ANALYZER_FILES) $(ANALYZER_PROPERTIES)
	@echo "================= $@"
	time $(JAVAC) -classpath $(anmodule_JAR) $(JAVAC_FLAGS) -d . $(ANALYZER_FILES)
	echo "Create $(ANALYZER_JAR) in `pwd`"
	$(JAR) $(JAR_FLAGS) $@ $(ANALYZER_MF) $(ANALYZER_CLASSES) \
			       $(ANALYZER_PROPERTIES)


# Rules for generating a jar file implementing Java collector API
#
collector_FILES     = $(srcdir)/$(JAVA_PROFAPI_DIR)/CollectorAPI.java
collector_MF        = $(srcdir)/gprofng-collector.mf
collector_JAR       = gprofng-collector.jar
collector_CLASSES   = $(collector_FILES:$(srcdir)/$(JAVA_PROFAPI_DIR)/%.java=$(JAVA_PROFAPI_DIR)/%.class)

$(collector_JAR): $(JAVA_PROFAPI_DIR) $(collector_FILES) $(collector_MF)
	@echo "================= $@"
	$(JAVAC) $(JAVAC_FLAGS) -d . $(collector_FILES)
	$(JAR) $(JAR_FLAGS) $@ $(collector_MF) $(collector_CLASSES)

bin_SCRIPTS = gp-display-gui
CLEANFILES = $(bin_SCRIPTS) $(ANALYZER_JAR) $(GPROFNG_CLASSES)

gp-display-gui: $(srcdir)/gprofng-gui.sh
	@echo "================= $@"
	sed -e "s~REPLACE_ME_WITH_VERSION~$(VERSION)~" < $(srcdir)/gprofng-gui.sh \
	  | sed -e "s~GPROFNG_bindir=~GPROFNG_bindir='$(bindir)'~" \
	  | sed -e "s~GPROFNG_libdir=~GPROFNG_libdir='$(libdir)'~" \
	  | sed -e "s~GPROFNG_datadir=~GPROFNG_datadir='$(datadir)'~" \
	  > $@
	chmod +x $@

dist_pkgdata_DATA = $(ANALYZER_JAR) $(collector_JAR) $(anmodule_JAR)

DIST_FILES = aclocal.m4 analyzer_st.mf compile configure.ac configure \
	gprofng-collector.mf gprofng-gui.mf gprofng-gui.sh gprofng-gui.spec \
	gprofng.properties install-sh Makefile.am Makefile.in missing \
	org README tools

dist:
	@echo "================= $@"
	rm -f $(PACKAGE_NAME).$(VERSION).tar.gz
	( cd $(srcdir); gtar --transform "s/^/$(PACKAGE_NAME).$(VERSION)\//" \
	  -cf - $(DIST_FILES) ) > $(PACKAGE_NAME).$(VERSION).tar
	gzip $(PACKAGE_NAME).$(VERSION).tar


