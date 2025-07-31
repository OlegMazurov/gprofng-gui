# Copyright (C) 2022-2025 Free Software Foundation
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

AUTOMAKE_OPTIONS =
ACLOCAL_AMFLAGS = -I . 

JAVA    = @JAVA@
JAVAC   = @JAVAC@
JAR	= @JAR@
JAVA_FLAGS =
JAVAC_FLAGS =
JAR_FLAGS = cfvm

MPMT_DIR = org/gprofng/mpmt
ANALYZER_DIR = org/gprofng/analyzer
COLLECTOR_DIR = org/gprofng/collector

ANALYZER_JAR = gprofng-analyzer.jar
GPROFNG_JAR = gprofng.jar
COLLECTOR_JAR = gprofng-collector.jar

if BUILD_LINUX
GPROFNG_DISPLAY_GUI = gprofng-display-gui
INSTAL_EXEC_OS = install-exec-local-linux
DesktopLauncher = gprofng-gui.desktop
endif
if BUILD_OSX
GPROFNG_DISPLAY_GUI = gprofng-gui
INSTAL_EXEC_OS = install-exec-local-osx
DesktopLauncher = 
endif
if BUILD_WINDOWS
GPROFNG_DISPLAY_GUI = gprofng-gui.ps1
INSTAL_EXEC_OS = install-exec-local-windows
DesktopLauncher =
endif

MPMT_SRC  = \
	$(MPMT_DIR)/AboutPanel.java \
	$(MPMT_DIR)/AnAction.java \
	$(MPMT_DIR)/AnAddress.java \
	$(MPMT_DIR)/Analyzer.java \
	$(MPMT_DIR)/AnChooser.java \
	$(MPMT_DIR)/AnCompDisp.java \
	$(MPMT_DIR)/AnDialog.java \
	$(MPMT_DIR)/AnDisplay.java \
	$(MPMT_DIR)/AnDouble.java \
	$(MPMT_DIR)/AnEvent.java \
	$(MPMT_DIR)/AnFile.java \
	$(MPMT_DIR)/AnFileSystemView.java \
	$(MPMT_DIR)/AnFrame.java \
	$(MPMT_DIR)/AnHotGap.java \
	$(MPMT_DIR)/AnIconButton.java \
	$(MPMT_DIR)/AnInteger.java \
	$(MPMT_DIR)/AnListener.java \
	$(MPMT_DIR)/AnList.java \
	$(MPMT_DIR)/AnLocale.java \
	$(MPMT_DIR)/AnLong.java \
	$(MPMT_DIR)/AnMemoryManager.java \
	$(MPMT_DIR)/AnMetric.java \
	$(MPMT_DIR)/AnObject.java \
	$(MPMT_DIR)/AnShellCommand.java \
	$(MPMT_DIR)/AnString.java \
	$(MPMT_DIR)/AnTable.java \
	$(MPMT_DIR)/AnTree.java \
	$(MPMT_DIR)/AnVariable.java \
	$(MPMT_DIR)/AnWindow.java \
	$(MPMT_DIR)/CalledByCallsDisassemblyView.java \
	$(MPMT_DIR)/CalledByCallsDisp.java \
	$(MPMT_DIR)/CalledByCallsFunctionsView.java \
	$(MPMT_DIR)/CalledByCallsSourceView.java \
	$(MPMT_DIR)/CallerCalleesView.java \
	$(MPMT_DIR)/CallTreeView.java \
	$(MPMT_DIR)/CannotFindFilePanel.java \
	$(MPMT_DIR)/collect/CollectDialog.java \
	$(MPMT_DIR)/collect/Collector.java \
	$(MPMT_DIR)/collect/CollectorOpenPanel.java \
	$(MPMT_DIR)/collect/CollectPanel2.java \
	$(MPMT_DIR)/collect/CollectPanel3.java \
	$(MPMT_DIR)/collect/CollectPanel.java \
	$(MPMT_DIR)/collect/CollectUtility.java \
	$(MPMT_DIR)/collect/HWCEntry.java \
	$(MPMT_DIR)/collect/HWCSelectDialog.java \
	$(MPMT_DIR)/collect/HWCSelectPanel.java \
	$(MPMT_DIR)/compare/AnTransferHandler.java \
	$(MPMT_DIR)/compare/CompareAdvancedDialog.java \
	$(MPMT_DIR)/compare/CompareGroupsPanel.java \
	$(MPMT_DIR)/compare/CompareNavigationPanel.java \
	$(MPMT_DIR)/compare/CompareSimpleDialog.java \
	$(MPMT_DIR)/compare/CompareSimplePanel.java \
	$(MPMT_DIR)/compare/CompareStatusPanel.java \
	$(MPMT_DIR)/compare/ExperimentGroupsPanel.java \
	$(MPMT_DIR)/ConfigurationPanel.java \
	$(MPMT_DIR)/control_panel/CPDetailsScrollPane.java \
	$(MPMT_DIR)/control_panel/CPFilterPanelHandler.java \
	$(MPMT_DIR)/control_panel/CPLayoutPanel.java \
	$(MPMT_DIR)/control_panel/CPToolbarHandler.java \
	$(MPMT_DIR)/control_panel/CPToolbar.java \
	$(MPMT_DIR)/DataLayoutView.java \
	$(MPMT_DIR)/DataObjectsView.java \
	$(MPMT_DIR)/DisasmDisp.java \
	$(MPMT_DIR)/DisassemblyView.java \
	$(MPMT_DIR)/DualSourceView.java \
	$(MPMT_DIR)/event/AnChangeEvent.java \
	$(MPMT_DIR)/event/AnChangeListener.java \
	$(MPMT_DIR)/event/AnEventManager.java \
	$(MPMT_DIR)/event/AnSelectionEvent.java \
	$(MPMT_DIR)/ExperimentPickListElement.java \
	$(MPMT_DIR)/ExperimentPickList.java \
	$(MPMT_DIR)/ExperimentPickLists.java \
	$(MPMT_DIR)/experiment_props/DataDescriptor.java \
	$(MPMT_DIR)/experiment_props/ExperimentProperties.java \
	$(MPMT_DIR)/experiment_props/Experiments.java \
	$(MPMT_DIR)/experiment_props/PropDescriptor.java \
	$(MPMT_DIR)/ExperimentsView.java \
	$(MPMT_DIR)/export/ExportDialog.java \
	$(MPMT_DIR)/export/Export.java \
	$(MPMT_DIR)/export/ExportPanel.java \
	$(MPMT_DIR)/export/ExportSupport.java \
	$(MPMT_DIR)/filter/CustomEditPanel.java \
	$(MPMT_DIR)/filter/CustomFilterAction.java \
	$(MPMT_DIR)/filter/CustomFilterDialog.java \
	$(MPMT_DIR)/filter/CustomPanel.java \
	$(MPMT_DIR)/filter/FilterClause.java \
	$(MPMT_DIR)/filter/FilterNavigationPanel.java \
	$(MPMT_DIR)/filter/FilterParser.java \
	$(MPMT_DIR)/filter/Filters.java \
	$(MPMT_DIR)/filter/FilterStatusPanel.java \
	$(MPMT_DIR)/filter/FilterTextField.java \
	$(MPMT_DIR)/filter/KeywordsPanel.java \
	$(MPMT_DIR)/filter/RedoFilterAction.java \
	$(MPMT_DIR)/filter/RemoveAllFilterAction.java \
	$(MPMT_DIR)/filter/UndoFilterAction.java \
	$(MPMT_DIR)/flame/FlameBlock.java \
	$(MPMT_DIR)/flame/FlameData.java \
	$(MPMT_DIR)/flame/FlamePanel.java \
	$(MPMT_DIR)/flame/FlameRow.java \
	$(MPMT_DIR)/flame/FlameToolBar.java \
	$(MPMT_DIR)/flame/FlameView.java \
	$(MPMT_DIR)/FuncListDisp.java \
	$(MPMT_DIR)/FunctionsView.java \
	$(MPMT_DIR)/guitesting/GUITesting.java \
	$(MPMT_DIR)/HeapView.java \
	$(MPMT_DIR)/HotGapPanel.java \
	$(MPMT_DIR)/IDispToModel.java \
	$(MPMT_DIR)/IExtTreeModel.java \
	$(MPMT_DIR)/InstructionFrequencyView.java \
	$(MPMT_DIR)/IOView.java \
	$(MPMT_DIR)/ipc/IPCCancelledException.java \
	$(MPMT_DIR)/ipc/IPCContext.java \
	$(MPMT_DIR)/ipc/IPCErrorReader.java \
	$(MPMT_DIR)/ipc/IPCHandle.java \
	$(MPMT_DIR)/ipc/IPCListener.java \
	$(MPMT_DIR)/ipc/IPCLogger.java \
	$(MPMT_DIR)/ipc/IPCProtocol.java \
	$(MPMT_DIR)/ipc/IPCReader.java \
	$(MPMT_DIR)/ipc/IPCRequestHeader.java \
	$(MPMT_DIR)/ipc/IPCRequest.java \
	$(MPMT_DIR)/ipc/IPCResponseHeader.java \
	$(MPMT_DIR)/ipc/IPCResponse.java \
	$(MPMT_DIR)/ipc/IPCResult.java \
	$(MPMT_DIR)/ipc/IPCTester.java \
	$(MPMT_DIR)/IPC.java \
	$(MPMT_DIR)/IPCProcess.java \
	$(MPMT_DIR)/ITreeToPanel.java \
	$(MPMT_DIR)/KeyboardShortcuts.java \
	$(MPMT_DIR)/LibraryVisibilityDialog.java \
	$(MPMT_DIR)/LibraryVisibilityPanel.java \
	$(MPMT_DIR)/LinesView.java \
	$(MPMT_DIR)/mainview/BorderPanel.java \
	$(MPMT_DIR)/mainview/MainViewPanel.java \
	$(MPMT_DIR)/mainview/Panel.java \
	$(MPMT_DIR)/mainview/SubviewAreaPanel.java \
	$(MPMT_DIR)/mainview/Subview.java \
	$(MPMT_DIR)/MemoryIndexObjectView.java \
	$(MPMT_DIR)/metrics/BasicMetric.java \
	$(MPMT_DIR)/metrics/CompareBarData.java \
	$(MPMT_DIR)/metrics/IPCMetricsAPI.java \
	$(MPMT_DIR)/metrics/MetricAttr.java \
	$(MPMT_DIR)/metrics/MetricColors.java \
	$(MPMT_DIR)/metrics/MetricLabel.java \
	$(MPMT_DIR)/metrics/MetricNode.java \
	$(MPMT_DIR)/metrics/MetricOption.java \
	$(MPMT_DIR)/metrics/MetricsGUI.java \
	$(MPMT_DIR)/metrics/MetricValue.java \
	$(MPMT_DIR)/metrics/SelectableMetricNode.java \
	$(MPMT_DIR)/metrics/ValueMetricNode.java \
	$(MPMT_DIR)/MiniFuncListDisp.java \
	$(MPMT_DIR)/navigation/ActionPanel.java \
	$(MPMT_DIR)/navigation/NavigationPanel.java \
	$(MPMT_DIR)/navigation/PanelDropTargetListener.java \
	$(MPMT_DIR)/navigation/TopNavigationPanel.java \
	$(MPMT_DIR)/navigation/View.java \
	$(MPMT_DIR)/navigation/ViewPanel.java \
	$(MPMT_DIR)/navigation/ViewsPanel.java \
	$(MPMT_DIR)/NoMetricsSelectedPanel.java \
	$(MPMT_DIR)/overview/AnBarTree.java \
	$(MPMT_DIR)/overview/BarColorKey.java \
	$(MPMT_DIR)/overview/Bar.java \
	$(MPMT_DIR)/overview/BarPanel.java \
	$(MPMT_DIR)/overview/CheckBoxBarPanel.java \
	$(MPMT_DIR)/overview/GradientPanel.java \
	$(MPMT_DIR)/overview/OuterPanel.java \
	$(MPMT_DIR)/overview/Overview.java \
	$(MPMT_DIR)/overview/OverviewPanel.java \
	$(MPMT_DIR)/overview/OverviewView.java \
	$(MPMT_DIR)/overview/ProgressBarPanel.java \
	$(MPMT_DIR)/overview/TopButtonPanel.java \
	$(MPMT_DIR)/overview/TurnerLabelPanel.java \
	$(MPMT_DIR)/overview/TurnerPanel.java \
	$(MPMT_DIR)/PCsView.java \
	$(MPMT_DIR)/persistence/UserPref.java \
	$(MPMT_DIR)/persistence/UserPrefPersistence.java \
	$(MPMT_DIR)/picklist/PickListElement.java \
	$(MPMT_DIR)/picklist/PickListEvent.java \
	$(MPMT_DIR)/picklist/PickList.java \
	$(MPMT_DIR)/picklist/PickListListener.java \
	$(MPMT_DIR)/picklist/StringPickListElement.java \
	$(MPMT_DIR)/picklist/StringPickList.java \
	$(MPMT_DIR)/progress/CancelContext.java \
	$(MPMT_DIR)/progress/Cancellable.java \
	$(MPMT_DIR)/progress/InternalHandle.java \
	$(MPMT_DIR)/progress/ProgressHandle.java \
	$(MPMT_DIR)/progress/ProgressPanel.java \
	$(MPMT_DIR)/progress/SystemProgressPanel.java \
	$(MPMT_DIR)/PsParser.java \
	$(MPMT_DIR)/remote/Authentication.java \
	$(MPMT_DIR)/remote/AuthenticationPanel.java \
	$(MPMT_DIR)/remote/CheckBoxList.java \
	$(MPMT_DIR)/remote/CloseExperimentDialog.java \
	$(MPMT_DIR)/remote/CloseExperimentPanel.java \
	$(MPMT_DIR)/remote/ConnectionDialog.java \
	$(MPMT_DIR)/remote/ConnectionManager.java \
	$(MPMT_DIR)/remote/ConnectionPanel.java \
	$(MPMT_DIR)/ResolveFilePathDialog.java \
	$(MPMT_DIR)/ResolveFilePathPanel.java \
	$(MPMT_DIR)/SelectedObject.java \
	$(MPMT_DIR)/SelectionManager.java \
	$(MPMT_DIR)/settings/AnSettingChangeEvent.java \
	$(MPMT_DIR)/settings/CallTreePanel.java \
	$(MPMT_DIR)/settings/CallTreeSetting.java \
	$(MPMT_DIR)/settings/CompareModeSetting.java \
	$(MPMT_DIR)/settings/ExportSettingsDialog.java \
	$(MPMT_DIR)/settings/ExportSettingsPanel.java \
	$(MPMT_DIR)/settings/FormatSetting.java \
	$(MPMT_DIR)/settings/ImportSettingsDialog.java \
	$(MPMT_DIR)/settings/ImportSettingsPanel.java \
	$(MPMT_DIR)/settings/LibraryVisibilitySetting.java \
	$(MPMT_DIR)/settings/MetricAttributes.java \
	$(MPMT_DIR)/settings/MetricNameSelected.java \
	$(MPMT_DIR)/settings/MetricNameSelection.java \
	$(MPMT_DIR)/settings/MetricSelection.java \
	$(MPMT_DIR)/settings/MetricsPanel.java \
	$(MPMT_DIR)/settings/MetricsSetting.java \
	$(MPMT_DIR)/settings/MetricState.java \
	$(MPMT_DIR)/settings/MetricStates.java \
	$(MPMT_DIR)/settings/MetricType.java \
	$(MPMT_DIR)/settings/MMetric.java \
	$(MPMT_DIR)/settings/PathMapSetting.java \
	$(MPMT_DIR)/settings/PathmapsPanel.java \
	$(MPMT_DIR)/settings/SearchPathPanel.java \
	$(MPMT_DIR)/settings/SearchPathSetting.java \
	$(MPMT_DIR)/settings/Setting.java \
	$(MPMT_DIR)/settings/Settings.java \
	$(MPMT_DIR)/settings/SourceDisassemblySetting.java \
	$(MPMT_DIR)/settings/TableSettings.java \
	$(MPMT_DIR)/settings/TimelineSetting.java \
	$(MPMT_DIR)/settings/ViewModeEnabledSetting.java \
	$(MPMT_DIR)/settings/ViewModeSetting.java \
	$(MPMT_DIR)/settings/ViewsSetting.java \
	$(MPMT_DIR)/SourceDisassemblyView.java \
	$(MPMT_DIR)/SourceDisp.java \
	$(MPMT_DIR)/SourceView.java \
	$(MPMT_DIR)/statecolors/AnColorChooser.java \
	$(MPMT_DIR)/statecolors/ChooserStateView.java \
	$(MPMT_DIR)/statecolors/ColorMap.java \
	$(MPMT_DIR)/statecolors/ColorRule.java \
	$(MPMT_DIR)/statecolors/StackState.java \
	$(MPMT_DIR)/statecolors/StackStateList.java \
	$(MPMT_DIR)/statecolors/StackView.java \
	$(MPMT_DIR)/statecolors/StackViewState.java \
	$(MPMT_DIR)/statecolors/StateColorMap.java \
	$(MPMT_DIR)/StatisticsView.java \
	$(MPMT_DIR)/statuspanel/StatusComponentHandle.java \
	$(MPMT_DIR)/statuspanel/StatusComponent.java \
	$(MPMT_DIR)/statuspanel/StatusHandleFactory.java \
	$(MPMT_DIR)/statuspanel/StatusLabelHandle.java \
	$(MPMT_DIR)/statuspanel/StatusLabel.java \
	$(MPMT_DIR)/statuspanel/StatusLabelValueHandle.java \
	$(MPMT_DIR)/statuspanel/StatusPanel.java \
	$(MPMT_DIR)/SummaryPanel.java \
	$(MPMT_DIR)/table/AnJTable.java \
	$(MPMT_DIR)/table/AnTableHeaderPanel.java \
	$(MPMT_DIR)/table/AnTableScrollPane.java \
	$(MPMT_DIR)/table/AttributePanel.java \
	$(MPMT_DIR)/table/ExclusiveAndInclusivePanel.java \
	$(MPMT_DIR)/table/ExclusiveOrInclusivePanel.java \
	$(MPMT_DIR)/table/HeaderMouseHandler.java \
	$(MPMT_DIR)/table/MetricPanelDropTargetListener.java \
	$(MPMT_DIR)/table/MetricPanel.java \
	$(MPMT_DIR)/timeline2/cursorevent/CursorEvent.java \
	$(MPMT_DIR)/timeline2/cursorevent/CursorMoveEvent.java \
	$(MPMT_DIR)/timeline2/cursorevent/CursorSetEvent.java \
	$(MPMT_DIR)/timeline2/data/GenericEvent.java \
	$(MPMT_DIR)/timeline2/data/SampleEvent.java \
	$(MPMT_DIR)/timeline2/data/StackEvent.java \
	$(MPMT_DIR)/timeline2/Entity.java \
	$(MPMT_DIR)/timeline2/EventDetails.java \
	$(MPMT_DIR)/timeline2/IconRuler.java \
	$(MPMT_DIR)/timeline2/MetaExperiment.java \
	$(MPMT_DIR)/timeline2/RowData.java \
	$(MPMT_DIR)/timeline2/RowDataRequestParams.java \
	$(MPMT_DIR)/timeline2/RowDefinition.java \
	$(MPMT_DIR)/timeline2/RowGeometry.java \
	$(MPMT_DIR)/timeline2/TimelineDraw.java \
	$(MPMT_DIR)/timeline2/TimelineSelectionEvent.java \
	$(MPMT_DIR)/timeline2/TimelineSelectionRowEvent.java \
	$(MPMT_DIR)/timeline2/TimelineVariable.java \
	$(MPMT_DIR)/timeline2/TimelineView.java \
	$(MPMT_DIR)/timeline2/TL2ControlPanel.java \
	$(MPMT_DIR)/timeline2/TL2CoordCalcData.java \
	$(MPMT_DIR)/timeline2/TL2DataFetcher.java \
	$(MPMT_DIR)/timeline2/TL2DataSnapshot.java \
	$(MPMT_DIR)/timeline2/TL2Drawer.java \
	$(MPMT_DIR)/timeline2/TLExperiment.java \
	$(MPMT_DIR)/timeline_common/CoordCalcDataImpl.java \
	$(MPMT_DIR)/timeline_common/CoordCalcDataMaster.java \
	$(MPMT_DIR)/timeline_common/CoordCalcDataReader.java \
	$(MPMT_DIR)/timeline_common/CoordCalcTimeImpl.java \
	$(MPMT_DIR)/timeline_common/CoordCalcTimeMaster.java \
	$(MPMT_DIR)/timeline_common/CoordCalcTimeReader.java \
	$(MPMT_DIR)/timeline_common/TimelineCaliper.java \
	$(MPMT_DIR)/timeline_common/TimelineCursor.java \
	$(MPMT_DIR)/timeline_common/TimelineDrawer.java \
	$(MPMT_DIR)/timeline_common/TimelinePanel.java \
	$(MPMT_DIR)/timeline_common/TimelineSelectionCaliperEvent.java \
	$(MPMT_DIR)/timeline_common/TimelineSelectionGenericEvent.java \
	$(MPMT_DIR)/timeline_common/TimelineSelectionHistoryEvent.java \
	$(MPMT_DIR)/timeline_common/TimelineSelectionListener.java \
	$(MPMT_DIR)/timeline_common/VerticalRowRuler.java \
	$(MPMT_DIR)/timeline/events/DeadlockEvent.java \
	$(MPMT_DIR)/timeline/events/DetailsIPC.java \
	$(MPMT_DIR)/timeline/events/DurationEvent.java \
	$(MPMT_DIR)/timeline/events/EventDetail.java \
	$(MPMT_DIR)/timeline/events/ExtendedEvent.java \
	$(MPMT_DIR)/timeline/events/HeapEvent.java \
	$(MPMT_DIR)/timeline/events/HWCEvent.java \
	$(MPMT_DIR)/timeline/events/IOEvent.java \
	$(MPMT_DIR)/timeline/events/MPIEvent.java \
	$(MPMT_DIR)/timeline/events/ProfileEvent.java \
	$(MPMT_DIR)/timeline/events/RaceEvent.java \
	$(MPMT_DIR)/timeline/events/Sample.java \
	$(MPMT_DIR)/timeline/events/StateEvent.java \
	$(MPMT_DIR)/timeline/events/SyncEvent.java \
	$(MPMT_DIR)/toolbar/FindTextPanel.java \
	$(MPMT_DIR)/toolbar/ToolBarFiller.java \
	$(MPMT_DIR)/toolbar/ToolBarPanel.java \
	$(MPMT_DIR)/toolbar/ToolBarSeparator.java \
	$(MPMT_DIR)/util/gui/ActionTextField.java \
	$(MPMT_DIR)/util/gui/AnBorderPanel.java \
	$(MPMT_DIR)/util/gui/AnCheckBox.java \
	$(MPMT_DIR)/util/gui/AnDialog2.java \
	$(MPMT_DIR)/util/gui/AnDialogOuterPanel.java \
	$(MPMT_DIR)/util/gui/AnGradientPanel.java \
	$(MPMT_DIR)/util/gui/AnInsetsPanel.java \
	$(MPMT_DIR)/util/gui/AnJList.java \
	$(MPMT_DIR)/util/gui/AnJPanel.java \
	$(MPMT_DIR)/util/gui/AnJScrollPane.java \
	$(MPMT_DIR)/util/gui/AnJTree.java \
	$(MPMT_DIR)/util/gui/AnLongScrollBar.java \
	$(MPMT_DIR)/util/gui/AnMenuBar.java \
	$(MPMT_DIR)/util/gui/AnScrollBarUI.java \
	$(MPMT_DIR)/util/gui/AnSplitPaneFixedRightSize.java \
	$(MPMT_DIR)/util/gui/AnSplitPaneInternal.java \
	$(MPMT_DIR)/util/gui/AnSplitPane.java \
	$(MPMT_DIR)/util/gui/AnTabbedPane.java \
	$(MPMT_DIR)/util/gui/AnTabbedPaneUI.java \
	$(MPMT_DIR)/util/gui/AnTextIcon.java \
	$(MPMT_DIR)/util/gui/AnUtility.java \
	$(MPMT_DIR)/util/gui/RainbowColorScale.java \
	$(MPMT_DIR)/util/gui/ToolTipPopup.java \
	$(MPMT_DIR)/util/ruler/LabelCoordinateCalculator.java \
	$(MPMT_DIR)/util/ruler/RangeRuler.java \
	$(MPMT_DIR)/util/ruler/Ruler.java \
	$(MPMT_DIR)/util/ruler/valuetypes/ValuesBytes.java \
	$(MPMT_DIR)/util/ruler/valuetypes/ValuesGeneric.java \
	$(MPMT_DIR)/util/ruler/valuetypes/ValuesLabelList.java \
	$(MPMT_DIR)/util/ruler/valuetypes/ValuesLong.java \
	$(MPMT_DIR)/util/ruler/valuetypes/ValuesNanoseconds.java \
	$(MPMT_DIR)/util/ruler/valuetypes/ValuesWithUnits.java \
	$(MPMT_DIR)/util/zoomruler/OverlayMouseHandler.java \
	$(MPMT_DIR)/util/zoomruler/PaintListener.java \
	$(MPMT_DIR)/util/zoomruler/ZoomRulerComponent.java \
	$(MPMT_DIR)/util/zoomruler/ZoomRulerEvent.java \
	$(MPMT_DIR)/util/zoomruler/ZoomRulerListener.java \
	$(MPMT_DIR)/util/zoomruler/ZoomRulerOverlay.java \
	$(MPMT_DIR)/ViewDisplayPanel.java \
	$(MPMT_DIR)/welcome/WelcomeButton.java \
	$(MPMT_DIR)/welcome/WelcomeInnerPanel.java \
	$(MPMT_DIR)/welcome/WelcomePanel.java \
	$(MPMT_DIR)/welcome/WelcomeView.java

MPMT_FILES  = \
	$(MPMT_SRC:%.java=$(srcdir)/%.java) \
	$(ANALYZER_FILES)

ICONS = \
	$(MPMT_DIR)/icons/add.png \
	$(MPMT_DIR)/icons/addtablecolumns.png \
	$(MPMT_DIR)/icons/analyzer64.png \
	$(MPMT_DIR)/icons/apply_filter.png \
	$(MPMT_DIR)/icons/apply_mtrs.png \
	$(MPMT_DIR)/icons/attributed.png \
	$(MPMT_DIR)/icons/back_filter.png \
	$(MPMT_DIR)/icons/backward.png \
	$(MPMT_DIR)/icons/badexperiment.png \
	$(MPMT_DIR)/icons/base1.png \
	$(MPMT_DIR)/icons/base2.png \
	$(MPMT_DIR)/icons/blank.png \
	$(MPMT_DIR)/icons/bubble_icon_16.png \
	$(MPMT_DIR)/icons/callee.png \
	$(MPMT_DIR)/icons/caller.png \
	$(MPMT_DIR)/icons/cancelProcess.png \
	$(MPMT_DIR)/icons/class.png \
	$(MPMT_DIR)/icons/collect.png \
	$(MPMT_DIR)/icons/color.png \
	$(MPMT_DIR)/icons/compareAbsolute.png \
	$(MPMT_DIR)/icons/compareDelta.png \
	$(MPMT_DIR)/icons/compareHamburger.png \
	$(MPMT_DIR)/icons/compare.png \
	$(MPMT_DIR)/icons/compareRatio.png \
	$(MPMT_DIR)/icons/compareReverse.png \
	$(MPMT_DIR)/icons/connect.png \
	$(MPMT_DIR)/icons/data.png \
	$(MPMT_DIR)/icons/delete.png \
	$(MPMT_DIR)/icons/down.png \
	$(MPMT_DIR)/icons/errorNew.png \
	$(MPMT_DIR)/icons/error.png \
	$(MPMT_DIR)/icons/errorWarningNew.png \
	$(MPMT_DIR)/icons/exclusive.png \
	$(MPMT_DIR)/icons/exe_elf.png \
	$(MPMT_DIR)/icons/expandcollaps.png \
	$(MPMT_DIR)/icons/experiment.png \
	$(MPMT_DIR)/icons/expgroup.png \
	$(MPMT_DIR)/icons/fileResolved.png \
	$(MPMT_DIR)/icons/fileUnResolved.png \
	$(MPMT_DIR)/icons/filter.png \
	$(MPMT_DIR)/icons/find_next.png \
	$(MPMT_DIR)/icons/find_prev.png \
	$(MPMT_DIR)/icons/forward_filter.png \
	$(MPMT_DIR)/icons/forward.png \
	$(MPMT_DIR)/icons/func_item.png \
	$(MPMT_DIR)/icons/gear.png \
	$(MPMT_DIR)/icons/gobackward.png \
	$(MPMT_DIR)/icons/godown.png \
	$(MPMT_DIR)/icons/goforward.png \
	$(MPMT_DIR)/icons/goup.png \
	$(MPMT_DIR)/icons/green100.png \
	$(MPMT_DIR)/icons/hamburgerBlank.png \
	$(MPMT_DIR)/icons/hamburgerFocused.png \
	$(MPMT_DIR)/icons/hamburger.png \
	$(MPMT_DIR)/icons/heaptrace.png \
	$(MPMT_DIR)/icons/hollowArrowDown.png \
	$(MPMT_DIR)/icons/hollowArrowLeft.png \
	$(MPMT_DIR)/icons/hollowArrowRight.png \
	$(MPMT_DIR)/icons/hollowArrowUp.png \
	$(MPMT_DIR)/icons/hot.png \
	$(MPMT_DIR)/icons/hwc.png \
	$(MPMT_DIR)/icons/inclusive.png \
	$(MPMT_DIR)/icons/i_o_usage_16.png \
	$(MPMT_DIR)/icons/jar.png \
	$(MPMT_DIR)/icons/more_icon.png \
	$(MPMT_DIR)/icons/mpitrace.png \
	$(MPMT_DIR)/icons/nav_bar_arrow.png \
	$(MPMT_DIR)/icons/nextview.png \
	$(MPMT_DIR)/icons/number.png \
	$(MPMT_DIR)/icons/oldexperiment.png \
	$(MPMT_DIR)/icons/open.png \
	$(MPMT_DIR)/icons/panhand.png \
	$(MPMT_DIR)/icons/percent.png \
	$(MPMT_DIR)/icons/previousview.png \
	$(MPMT_DIR)/icons/profile.png \
	$(MPMT_DIR)/icons/races.png \
	$(MPMT_DIR)/icons/redo.png \
	$(MPMT_DIR)/icons/removeBlank.png \
	$(MPMT_DIR)/icons/removeFocused.png \
	$(MPMT_DIR)/icons/remove.png \
	$(MPMT_DIR)/icons/reset.png \
	$(MPMT_DIR)/icons/sample.png \
	$(MPMT_DIR)/icons/save.png \
	$(MPMT_DIR)/icons/smallBlank.png \
	$(MPMT_DIR)/icons/small_down_focused.png \
	$(MPMT_DIR)/icons/small_down.png \
	$(MPMT_DIR)/icons/small_left.png \
	$(MPMT_DIR)/icons/small_right.png \
	$(MPMT_DIR)/icons/small_up_focused.png \
	$(MPMT_DIR)/icons/small_up.png \
	$(MPMT_DIR)/icons/stop.png \
	$(MPMT_DIR)/icons/synctrace.png \
	$(MPMT_DIR)/icons/tab.png \
	$(MPMT_DIR)/icons/time.png \
	$(MPMT_DIR)/icons/undo.png \
	$(MPMT_DIR)/icons/up.png \
	$(MPMT_DIR)/icons/values.png \
	$(MPMT_DIR)/icons/warnexperiment.png \
	$(MPMT_DIR)/icons/warningNew.png \
	$(MPMT_DIR)/icons/warning.png \
	$(MPMT_DIR)/icons/welcomeBackgroundShadow.png \
	$(MPMT_DIR)/icons/zoom_in.png \
	$(MPMT_DIR)/icons/zoom_out.png \
	$(MPMT_DIR)/icons/circleBlue.gif \
	$(MPMT_DIR)/icons/circleGreen.gif \
	$(MPMT_DIR)/icons/circleOrange.gif \
	$(MPMT_DIR)/icons/circleRed.gif \
	$(MPMT_DIR)/icons/performanceAnalyzerSplash.gif

GPROFNG_CLASSES = \
	$(MPMT_DIR)/*.class \
	$(MPMT_DIR)/*/*.class \
	$(MPMT_DIR)/*/*/*.class \
	$(MPMT_DIR)/*/*/*/*.class

GPROFNG_FORMS = \
	$(MPMT_DIR)/AboutPanel.form \
	$(MPMT_DIR)/CannotFindFilePanel.form \
	$(MPMT_DIR)/collect/CollectorOpenPanel.form \
	$(MPMT_DIR)/collect/CollectPanel2.form \
	$(MPMT_DIR)/collect/CollectPanel3.form \
	$(MPMT_DIR)/collect/HWCSelectPanel.form \
	$(MPMT_DIR)/compare/CompareGroupsPanel.form \
	$(MPMT_DIR)/compare/CompareSimplePanel.form \
	$(MPMT_DIR)/compare/CompareStatusPanel.form \
	$(MPMT_DIR)/compare/ExperimentGroupsPanel.form \
	$(MPMT_DIR)/ConfigurationPanel.form \
	$(MPMT_DIR)/export/ExportPanel.form \
	$(MPMT_DIR)/filter/CustomPanel.form \
	$(MPMT_DIR)/filter/FilterNavigationPanel.form \
	$(MPMT_DIR)/filter/FilterStatusPanel.form \
	$(MPMT_DIR)/filter/KeywordsPanel.form \
	$(MPMT_DIR)/LibraryVisibilityPanel.form \
	$(MPMT_DIR)/mainview/BorderPanel.form \
	$(MPMT_DIR)/mainview/MainViewPanel.form \
	$(MPMT_DIR)/navigation/ActionPanel.form \
	$(MPMT_DIR)/navigation/TopNavigationPanel.form \
	$(MPMT_DIR)/navigation/ViewPanel.form \
	$(MPMT_DIR)/navigation/ViewsPanel.form \
	$(MPMT_DIR)/NoMetricsSelectedPanel.form \
	$(MPMT_DIR)/overview/BarPanel.form \
	$(MPMT_DIR)/overview/CheckBoxBarPanel.form \
	$(MPMT_DIR)/overview/GradientPanel.form \
	$(MPMT_DIR)/overview/OuterPanel.form \
	$(MPMT_DIR)/overview/OverviewPanel.form \
	$(MPMT_DIR)/overview/ProgressBarPanel.form \
	$(MPMT_DIR)/overview/TopButtonPanel.form \
	$(MPMT_DIR)/overview/TurnerLabelPanel.form \
	$(MPMT_DIR)/overview/TurnerPanel.form \
	$(MPMT_DIR)/progress/ProgressPanel.form \
	$(MPMT_DIR)/remote/AuthenticationPanel.form \
	$(MPMT_DIR)/remote/CloseExperimentPanel.form \
	$(MPMT_DIR)/remote/ConnectionPanel.form \
	$(MPMT_DIR)/ResolveFilePathPanel.form \
	$(MPMT_DIR)/settings/CallTreePanel.form \
	$(MPMT_DIR)/settings/ExportSettingsPanel.form \
	$(MPMT_DIR)/settings/ImportSettingsPanel.form \
	$(MPMT_DIR)/settings/MetricsPanel.form \
	$(MPMT_DIR)/settings/PathmapsPanel.form \
	$(MPMT_DIR)/settings/SearchPathPanel.form \
	$(MPMT_DIR)/statuspanel/StatusPanel.form \
	$(MPMT_DIR)/util/gui/AnDialogOuterPanel.form \
	$(MPMT_DIR)/util/gui/AnInsetsPanel.form \
	$(MPMT_DIR)/welcome/WelcomeInnerPanel.form \
	$(MPMT_DIR)/welcome/WelcomePanel.form

anmodule_MF		= $(srcdir)/gprofng-gui.mf

MPMT_PROPERTIES = $(MPMT_DIR)/Bundle.properties

LocaleString.class: $(srcdir)/tools/LocaleString.java
	$(AM_V_GEN)$(JAVAC) $(JAVAC_FLAGS) -d . $(srcdir)/tools/LocaleString.java

$(MPMT_PROPERTIES): $(MPMT_FILES) LocaleString.class \
		      $(srcdir)/gprofng.properties
	$(AM_V_GEN)mkdir -m 777 -p $(@D) || echo "mkdir -m 777 -p $(@D) failed"
	echo "# THIS FILE IS AUTO-GENERATED. DON'T EDIT IT." > $@
	echo "" >> $@
	echo "gui_version=$(VERSION)" >> $@
	cat $(srcdir)/gprofng.properties >> $@
	( cd $(srcdir); cat $(MPMT_SRC) $(ANALYZER_SRC) ) | $(JAVA) $(JAVA_FLAGS) -cp . LocaleString >> $@

$(GPROFNG_JAR): $(anmodule_MF) $(MPMT_FILES) \
		 $(MPMT_PROPERTIES) $(ICONS:%=$(srcdir)/%)
	$(AM_V_GEN)rm -f $(GPROFNG_CLASSES)
	$(AM_V_at)$(JAVAC) -cp $(srcdir) $(JAVAC_FLAGS) -d . $(srcdir)/$(MPMT_DIR)/Analyzer.java
	$(AM_V_at)$(JAR) $(JAR_FLAGS) $@ $(anmodule_MF) $(MPMT_PROPERTIES) $(MPMT_DIR) \
		-C $(srcdir) $(MPMT_DIR)/icons

#
# Standalone gprofng GUI
#
ANALYZER_SRC    = \
	$(ANALYZER_DIR)/AnEnvironment.java \
	$(ANALYZER_DIR)/AnLog.java \
	$(ANALYZER_DIR)/AnMain.java
ANALYZER_FILES  = $(ANALYZER_SRC:%.java=$(srcdir)/%.java)
ANALYZER_MF     = $(srcdir)/analyzer_st.mf

ANALYZER_CLASSES = $(ANALYZER_DIR)/*.class
ANALYZER_PROPERTIES = $(ANALYZER_DIR)/Bundle.properties

$(ANALYZER_PROPERTIES): $(ANALYZER_FILES) LocaleString.class
	$(AM_V_GEN)mkdir -m 777 -p $(@D) || echo "mkdir -m 777 -p $(@D) failed"
	@echo "# THIS FILE IS AUTO-GENERATED. DON'T EDIT IT." > $@
	@echo "" >> $@
	$(AM_V_GEN)( cd $(srcdir); cat $(ANALYZER_SRC) ) | \
	  $(JAVA) $(JAVA_FLAGS) -cp . LocaleString >> $@

$(ANALYZER_JAR): $(GPROFNG_JAR) \
		    $(ANALYZER_MF) $(ANALYZER_FILES) $(ANALYZER_PROPERTIES)
	@echo "================= $@"
	$(AM_V_GEN)rm -f $(ANALYZER_CLASSES)
	$(AM_V_at)d=`pwd` ; (cd $(srcdir) ; $(JAVAC) -classpath $$d/$(GPROFNG_JAR) \
	  $(JAVAC_FLAGS) -d $$d $(ANALYZER_SRC) )
	$(AM_V_at)$(JAR) $(JAR_FLAGS) $@ $(ANALYZER_MF) $(ANALYZER_CLASSES) \
			       $(ANALYZER_PROPERTIES)

# Rules for generating a jar file implementing Java collector API
#
COLLECTOR_SRC      = $(COLLECTOR_DIR)/CollectorAPI.java
collector_FILES    = $(COLLECTOR_SRC:%.java=$(srcdir)/%.java)
collector_MF       = $(srcdir)/gprofng-collector.mf
collector_CLASSES  = $(COLLECTOR_DIR)/*.class

$(COLLECTOR_JAR): $(GPROFNG_JAR) $(collector_FILES) $(collector_MF)
	@echo "================= $@"
	$(AM_V_GEN)rm -f $(collector_CLASSES)
	$(AM_V_at)$(JAVAC) $(JAVAC_FLAGS) -d . $(collector_FILES)
	$(AM_V_at)$(JAR) $(JAR_FLAGS) $@ $(collector_MF) $(collector_CLASSES)

bin_SCRIPTS = $(GPROFNG_DISPLAY_GUI)
CLEANFILES = $(GPROFNG_DISPLAY_GUI) $(GPROFNG_JAR) $(ANALYZER_JAR) $(COLLECTOR_JAR) \
	$(GPROFNG_CLASSES) $(ANALYZER_CLASSES) $(collector_CLASSES) \
	$(ANALYZER_PROPERTIES) $(MPMT_PROPERTIES) LocaleString*.class \
	$(DesktopLauncher)

gprofng-display-gui: $(srcdir)/gprofng-gui.sh
	$(AM_V_GEN)sed -e "s~REPLACE_ME_WITH_VERSION~$(VERSION)~" < $(srcdir)/gprofng-gui.sh \
	  | sed -e "s~GPROFNG_bindir=~GPROFNG_bindir='$(bindir)'~" \
	  | sed -e "s~GPROFNG_libdir=~GPROFNG_libdir='$(libdir)'~" \
	  | sed -e "s~GPROFNG_datadir=~GPROFNG_datadir='$(datadir)'~" \
	  > $@
	$(AM_V_at)chmod +x $@

gprofng-gui.ps1:
	@echo "java -Xmx1024m -jar $(datadir)/$(PACKAGE)/gprofng-analyzer.jar \$Args" > $@
	$(AM_V_GEN)chmod +x $@

gprofng-gui:
	@echo "#!/bin/sh" > $@
	@echo "java -Xmx1024m -jar $(datadir)/$(PACKAGE)/gprofng-analyzer.jar \$@" >> $@
	$(AM_V_GEN)chmod +x $@

pkgdata_DATA = $(ANALYZER_JAR) $(COLLECTOR_JAR) $(GPROFNG_JAR)

PNG_DIR = $(srcdir)/images
PNG_FILES = $(PNG_DIR)/gprofng-32x32.png $(PNG_DIR)/gprofng-48x48.png \
	$(PNG_DIR)/gprofng-48x48.png $(PNG_DIR)/gprofng-64x64.png \
	$(PNG_DIR)/gprofng-128x128.png $(PNG_DIR)/gprofng-256x256.png
CLR_DIR = $(DESTDIR)$(datarootdir)/icons/hicolor

EXTRA_DIST = $(MPMT_SRC) $(ANALYZER_SRC) $(COLLECTOR_SRC) \
	$(ICONS) $(GPROFNG_FORMS) $(PNG_FILES) \
	aclocal.m4 analyzer_st.mf compile configure.ac configure \
	gprofng-collector.mf gprofng-gui.mf gprofng-gui.sh gprofng-gui.spec \
	gprofng.properties install-sh Makefile.am Makefile.in missing \
	$(PNG_DIR)/gprofng-128x128.ico $(PNG_DIR)/gprofng.icns \
	$(PNG_DIR)/installer-README $(PNG_DIR)/gprofng.iconset/icon_32.png \
	$(PNG_DIR)/gprofng.iconset/icon_48.png \
	$(PNG_DIR)/gprofng.iconset/icon_64.png \
	$(PNG_DIR)/gprofng.iconset/icon_128.png \
	$(PNG_DIR)/gprofng.iconset/icon_256.png \
	README AUTHORS NEWS tools

$(DesktopLauncher):
	@echo '[Desktop Entry]' > $@
	@echo "Encoding=UTF-8" >> $@
	@echo "Version=$(VERSION)" >> $@
	@echo "Type=Application" >> $@
	@echo "Name=gprofngg gui" >> $@
	@echo "Exec=$(prefix)/bin/gprofng-display-gui" >> $@
	@echo "Icon=gprofng" >> $@
	@echo "Categories=Profiling;" >> $@
	@echo "Comment=Graphical tool to create and analyze gprofng experiments" >> $@

PHONY: install-exec-local install-exec-local-linux install-exec-local-windows \
	install-exec-local-osx
install-exec-local: $(INSTAL_EXEC_OS)

install-exec-local-linux: $(DesktopLauncher) $(PNG_FILES)
	$(AM_V_GEN)for d in 32x32 48x48 64x64 128x128 256x256 ; do \
	  $(mkinstalldirs) $(CLR_DIR)/$$d/apps ; \
	  cp $(PNG_DIR)/gprofng-$$d.png $(CLR_DIR)/$$d/apps/gprofng.png ; \
	  chmod 644 $(CLR_DIR)/$$d/apps/gprofng.png ; \
	done
	$(AM_V_at)$(mkinstalldirs) $(DESTDIR)$(datarootdir)/applications
	$(AM_V_at)cp $(DesktopLauncher) $(DESTDIR)$(datarootdir)/applications/
	$(AM_V_at)chmod 644 $(DESTDIR)$(datarootdir)/applications/$(DesktopLauncher)
	$(AM_V_at)$(mkinstalldirs) $(DESTDIR)$(bindir)
	$(AM_V_at)rm -f $(DESTDIR)$(bindir)/gp-display-gui
	$(AM_V_at)ln -s gprofng-display-gui $(DESTDIR)$(bindir)/gp-display-gui

install-exec-local-windows install-exec-local-osx:
	@echo "================= $@"

uninstall-local:
	rm -f $(CLR_DIR)/*/apps/gprofng.png $(DESTDIR)$(bindir)/gp-display-gui \
	  $(DESTDIR)$(datarootdir)/applications/$(DesktopLauncher)

include doc/local.mk
