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
		<meta charset="utf-8"/>
		<link rel="stylesheet" href="{$stylePath}/de.interactive-instruments.min.css"/>
		<link rel="stylesheet" href="{$stylePath}/de.interactive-instruments.rep.css"/>
		<link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/jquerymobile/1.4.5/jquery.mobile.min.css"/>
		<script src="http://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.11.3.min.js"/>
		<script src="http://ajax.googleapis.com/ajax/libs/jquerymobile/1.4.5/jquery.mobile.js"/>
	</xsl:template>
	<!-- Report controls-->
	<!-- ########################################################################################## -->
	<xsl:template name="controls">
		<div id="rprtControl">
			<fieldset id="controlgroupLOD" data-role="controlgroup" data-mini="true">
				<legend>
					<xsl:value-of select="$lang/x:e[@key='LevelOfDetail']"/>
				</legend>
				<input type="radio" name="radio-LOD" id="cntrlAllDetails" value="cntrlAllDetails"/>
				<label for="cntrlAllDetails">
					<xsl:value-of select="$lang/x:e[@key='AllDetails']"/>
				</label>
				<input type="radio" name="radio-LOD" id="cntrlLessInformation" value="cntrlLessInformation"/>
				<label for="cntrlLessInformation">
					<xsl:value-of select="$lang/x:e[@key='LessInformation']"/>
				</label>
				<input type="radio" name="radio-LOD" id="cntrlSimplified" value="cntrlSimplified" checked="checked"/>
				<label for="cntrlSimplified">
					<xsl:value-of select="$lang/x:e[@key='Simplified']"/>
				</label>
			</fieldset>
			<fieldset id="controlgroupShow" data-role="controlgroup" data-mini="true">
				<legend>
					<xsl:value-of select="$lang/x:e[@key='Show']"/>
				</legend>
				
				<label for="cntrlShowAll">
					<xsl:value-of select="$lang/x:e[@key='All']"/>
				</label>
				<input type="radio" name="cntrlShowAll" id="cntrlShowAll" value="cntrlShowAll" checked="checked"/>
				
				<label for="cntrlShowOnlyFailed">
					<xsl:value-of select="$lang/x:e[@key='OnlyFailed']"/>
				</label>
				<input type="radio" name="cntrlShowOnlyFailed" id="cntrlShowOnlyFailed" value="cntrlShowOnlyFailed"/>
				
				<label for="cntrlShowOnlyManual">
					<xsl:value-of select="$lang/x:e[@key='OnlyManual']"/>
				</label>
				<input type="radio" name="cntrlShowOnlyManual" id="cntrlShowOnlyManual" value="cntrlShowOnlyManual"/>
				
			</fieldset>
		</div>
	</xsl:template>
	<!-- Script tags in the footer-->
	<!-- ########################################################################################## -->
	<xsl:template name="footerScripts">
		
		<div class="ui-field-contain" id="lodFadinMenu" style="display: none; width: 200px; position: fixed; top: 10px; right: 5px;" data-role="controlgroup">
			<label for="select-native-2"> <xsl:value-of select="$lang/x:e[@key='LevelOfDetail']"/> </label>	
			<select name="select-Show" id="lodFadinMenuSelect" data-mini="true">
				<option value="cntrlAllDetails"><xsl:value-of select="$lang/x:e[@key='AllDetails']"/></option>
				<option value="cntrlLessInformation"><xsl:value-of select="$lang/x:e[@key='LessInformation']"/></option>
				<option value="cntrlSimplified" selected="selected"><xsl:value-of select="$lang/x:e[@key='Simplified']"/></option>
			</select>
		</div>
		
		<script>
		
			<!-- Controls for switching the level of detail -->
			$("#controlgroupShow input").on( "click", function() {
				var cntrl = $( "#controlgroupShow input:checked" ).val();
				$('#lodFadinMenuSelect').val(cntrl).selectmenu('refresh');
				updateLod(cntrl);
			});
			
			$("#lodFadinMenuSelect").on( "change", function() {
				var cntrl = $( "#lodFadinMenuSelect option:selected").val()
				$("#"+cntrl).prop('checked', true).checkboxradio("refresh");
				// $("#"+cntrl).click();
				updateLod(cntrl);
			});	
			
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
			$( "input[name=radio-Show]" ).on( "click", function() {
			
			var cntrl = $( "input[name=radio-Show]:checked" ).val();
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
			
			
			$( document ).ready(function() {
				$('.ReportDetail').hide();
				$('.DoNotShowInSimpleView').hide();
				
				if ( $('.ManualTestCase, .ManualTestStep, .ManualAssertion').length==0) {
					$('#cntrlShowOnlyManual').checkboxradio('disable').checkboxradio("refresh");
				}
				console.log( "Manuals: " + $('.ManualTestCase, .ManualTestStep, .ManualAssertion').length );
				
				$(document).scroll(function () {
					var y = $(this).scrollTop();
					if (y > 370) {
						$('#lodFadinMenu').fadeIn();
					} else {
						$('#lodFadinMenu').fadeOut();
					}
				});
			});
			
			
		</script>
	</xsl:template>
</xsl:stylesheet>
