<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:x="http://www.interactive-instruments.de/etf/2.0" version="1.0" exclude-result-prefixes="x">
	<xsl:output method="html" doctype-system="about:legacy-compat" indent="yes" encoding="UTF-8"/>
	<xsl:variable name="defaultStyleResourcePath">
		<xsl:text>http://services.interactive-instruments.de/etf/css</xsl:text>
	</xsl:variable>
	<xsl:param name="stylePath" select="$defaultStyleResourcePath"/>
	<!-- JQuery Mobile and Styling includes-->
	<!-- ########################################################################################## -->
	<xsl:template name="jsfdeclAndCss">
		<!--link rel="stylesheet" href="{$stylePath}/de.interactive-instruments.min.css"/>
		<link rel="stylesheet" href="{$stylePath}/de.interactive-instruments.rep.css"/-->
		<link rel="stylesheet" href="http://resources.etf-validator.net/report/v2/css/de.interactive-instruments.min.css"/>
		<link rel="stylesheet" href="http://resources.etf-validator.net/report/v2/css/de.interactive-instruments.rep.css"/>
		<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jquerymobile/1.4.5/jquery.mobile.min.css"/>
		<script src="http://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.11.3.min.js"/>
		<script src="http://ajax.googleapis.com/ajax/libs/jquerymobile/1.4.5/jquery.mobile.min.js"/>
	</xsl:template>
	<!-- Report controls-->
	<!-- ########################################################################################## -->
	<xsl:template name="controls">
		<div id="rprtControl">
			<fieldset id="controlgroupLOD" data-role="controlgroup" data-mini="true">
				<legend>
					<xsl:value-of select="$lang/x:e[@key='LevelOfDetail']"/>
				</legend>
				
				<label for="cntrlAllDetails">
					<xsl:value-of select="$lang/x:e[@key='AllDetails']"/>
				</label>
				<input type="radio" name="radio-lod" id="cntrlAllDetails" value="cntrlAllDetails"/>
				
				<label for="cntrlLessInformation">
					<xsl:value-of select="$lang/x:e[@key='LessInformation']"/>
				</label>
				<input type="radio" name="radio-lod" id="cntrlLessInformation" value="cntrlLessInformation"/>
				
				<label for="cntrlSimplified">
					<xsl:value-of select="$lang/x:e[@key='Simplified']"/>
				</label>
				<input type="radio" name="radio-lod" id="cntrlSimplified" value="cntrlSimplified" checked="checked"/>
			</fieldset>
			<fieldset id="controlgroupShow" data-role="controlgroup" data-mini="true">
				<legend>
					<xsl:value-of select="$lang/x:e[@key='Show']"/>
				</legend>
				
				<label for="cntrlShowAll">
					<xsl:value-of select="$lang/x:e[@key='All']"/>
				</label>
				<input type="radio" name="radio-filter" id="cntrlShowAll" value="cntrlShowAll" checked="checked"/>
				
				<label for="cntrlShowOnlyFailed">
					<xsl:value-of select="$lang/x:e[@key='OnlyFailed']"/>
				</label>
				<input type="radio" name="radio-filter" id="cntrlShowOnlyFailed" value="cntrlShowOnlyFailed"/>
				
				<label for="cntrlShowOnlyManual">
					<xsl:value-of select="$lang/x:e[@key='OnlyManual']"/>
				</label>
				<input type="radio" name="radio-filter" id="cntrlShowOnlyManual" value="cntrlShowOnlyManual"/>
				
			</fieldset>
		</div>
	</xsl:template>
	<!-- Script tags in the footer-->
	<!-- ########################################################################################## -->
	<xsl:template name="footerScripts">
		
		<div class="ui-field-contain" id="lodFadinMenu" style="display: none; width: 200px; position: fixed; top: 10px; right: 5px;" data-role="controlgroup">
			<label for="select-Show"> <xsl:value-of select="$lang/x:e[@key='LevelOfDetail']"/> </label>	
			<select name="select-Show" id="lodFadinMenuSelect" data-mini="true">
				<option value="cntrlAllDetails"><xsl:value-of select="$lang/x:e[@key='AllDetails']"/></option>
				<option value="cntrlLessInformation"><xsl:value-of select="$lang/x:e[@key='LessInformation']"/></option>
				<option value="cntrlSimplified" selected="selected"><xsl:value-of select="$lang/x:e[@key='Simplified']"/></option>
			</select>
		</div>
		
		<script>
			
			function updateLod(cntrl) {
				if(cntrl=="cntrlSimplified")
				{
					$( ".ReportDetail" ).hide("slow");
					$('.DoNotShowInSimpleView').hide('slow');
					
					<!-- Cut text -->
					var assertionFailureMessage = 
					'Expected text value \'AssertionFailures:\' but was \'AssertionFailures:\'';
					$('.XQueryContainsAssertion').hide('slow');
				}
				else if(cntrl=="cntrlLessInformation")
				{
					$('.ReportDetail').hide('slow');
					$('.DoNotShowInSimpleView').show('slow');
				}
				else if(cntrl=="cntrlAllDetails")
				{
					$('.ReportDetail').show('slow');
					$('.DoNotShowInSimpleView').show('slow');
				}
			}
			
			<!-- Controls for filtering -->
			$( "input[name=radio-filter]" ).on( "click", function() {
			var cntrl = $( "input[name=radio-filter]:checked" ).val();
				if(cntrl=="cntrlShowOnlyFailed")
				{
					$('.TestSuite').collapsible('expand');
					$('.TestModule').collapsible('expand');
					$('.SuccessfulTestCase').hide('slow');
					$('.ManualTestCase').hide('slow');
					$('.FailedTestCase').collapsible('expand');
					$('.FailedTestCase').show('fast');
					$('.SuccessfulTestStep').collapsible('collapse');
					$('.ManualTestStep').collapsible('collapse');
					$('.FailedTestStep').collapsible('expand');
					$('.FailedTestStep').show('fast');
					$('.SuccessfulTestStep').hide('slow');
					$('.ManualTestStep').hide('slow');
					$('.FailedAssertion').collapsible('expand');
					$('.FailedAssertion').show('fast');
					$('.SuccessfulAssertion').hide('slow');
					$('.ManualAssertion').hide('slow');
				}
				else if(cntrl=="cntrlShowOnlyManual")
				{
					$('.TestSuite').collapsible('expand');
					$('.TestModule').collapsible('expand');
					$('.SuccessfulTestCase').hide('slow');
					$('.FailedTestCase').hide('slow');
					$('.ManualTestCase').collapsible('expand');
					$('.ManualTestCase').show('fast');
					$('.SuccessfulTestStep').collapsible('collapse');
					$('.FailedTestStep').collapsible('collapse');
					$('.ManualTestStep').collapsible('expand');
					$('.ManualTestStep').show('fast');
					$('.SuccessfulTestStep').hide('slow');
					$('.FailedTestStep').hide('slow');
					$('.ManualAssertion').collapsible('expand');
					$('.ManualAssertion').show('fast');
					$('.SuccessfulAssertion').hide('slow');
					$('.FailedAssertion').hide('slow');
				}
				else if(cntrl=="cntrlShowAll")
				{
					$('.SuccessfulTestCase').show('fast');
					$('.SuccessfulTestStep').show('fast');
					$('.SuccessfulAssertion').show('fast');
					$('.ManualTestCase').show('fast');
					$('.ManualTestStep').show('fast');
					$('.ManualAssertion').show('fast');
					$('.FailedTestCase').show('fast');
					$('.FailedTestStep').show('fast');
					$('.FailedAssertion').show('fast');
				}
			});
			
			$.fn.exists = function(){ return this.length > 0; }
			
			$.fn.getParentWithClass = function(className) {
				var p = this.parent();
				if(p.exists()) {
					return p.hasClass(className) ? p : p.getParentWithClass(className);
				}
				console.warn('Parent class '+className+' not found');
				return null;
			}
			
			<!-- Jump to element with ID -->
			function jumpToAnchor(anchorId) {
				var anchorElement = $('#'+anchorId);
				if(anchorId!="" &amp;&amp; anchorElement.exists()) {
					console.log("Scrolling to anchor: "+anchorId);
					// Expand parent model items

					anchorElement.collapsible('expand').getParentWithClass("TestSuite").collapsible('expand');
					var testModulePar = anchorElement.collapsible('expand').getParentWithClass("TestModule")
					if(testModulePar) {
						testModulePar.collapsible('expand');
					}
					var testCasePar = anchorElement.collapsible('expand').getParentWithClass("TestCase");
					if(testCasePar) {
						testCasePar.collapsible('expand');
					}
					var testStepPar = anchorElement.collapsible('expand').getParentWithClass("TestStep");
					if(testStepPar) {
						testStepPar.collapsible('expand');
					}
					var position = $(anchorElement).offset().top;
					$("html, body").stop().animate({ scrollTop: position });
					window.history.pushState(null,"", "#"+anchorId);
				}
			}			
			<!-- Controls for switching the level of detail -->
			$("#controlgroupLOD input").on( "click", function() {
				var cntrl = $( "#controlgroupLOD input:checked" ).val();
				$('#lodFadinMenuSelect').val(cntrl).selectmenu('refresh');
				updateLod(cntrl);
			});
			$("#lodFadinMenuSelect").on( "change", function() {
				var cntrl = $( "#lodFadinMenuSelect option:selected").val()
				$("#cntrlAllDetails").prop('checked', false).checkboxradio("refresh");
				$("#cntrlLessInformation").prop('checked', false).checkboxradio("refresh");
				$("#cntrlSimplified").prop('checked', false).checkboxradio("refresh");
				$("#"+cntrl).prop('checked', true).checkboxradio("refresh");
				updateLod(cntrl);
			});	
			$(document).scroll(function () {
				var y = $(this).scrollTop();
				if (y > 370) {
					$('#lodFadinMenu').fadeIn();
				} else {
					$('#lodFadinMenu').fadeOut();
				}
			});
			
			<!-- Init page -->
			$(document).one('pagebeforeshow', function() {
				$('.ReportDetail').hide();
				
				<!-- Jump to element with ID on page initialization -->
				var url = decodeURIComponent(window.location.href);
				var anchorIdx = url.indexOf("#");
				var anchorId = anchorIdx != -1 ? url.substring(anchorIdx+1) : "";
				var anchorElement = $('#'+anchorId);
				if(anchorId!="" &amp;&amp; anchorElement.exists()) {
					$('.DoNotShowInSimpleView').show();
					jumpToAnchor(anchorId);
					$("body").one("pagecontainershow", function () {
						var position = $(anchorElement).offset().top;
						// ... but it works...
						setTimeout( function() {
							$("html, body").stop().animate({ scrollTop: position });
							},150);
						});
				}else{
					$('.DoNotShowInSimpleView').hide();
				}
				
				
				<!-- Hide checkbox if there are no manual assertions -->
				if ( $('.ManualTestCase, .ManualTestStep, .ManualAssertion').length==0) {
					$('#cntrlShowOnlyManual').checkboxradio();
					$('#cntrlShowOnlyManual').checkboxradio('refresh');
					$('#cntrlShowOnlyManual').checkboxradio('disable').checkboxradio('refresh');
				}
			});
			
		</script>
	</xsl:template>
</xsl:stylesheet>
