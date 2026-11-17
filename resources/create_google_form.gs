/**
 * RoomSync — Google Form Automated Generator
 * 
 * This Google Apps Script creates the complete 5-section Google Form for RoomSync
 * matching the exact questions, types, and column headers expected by RoomSync's CSV Importer.
 * 
 * --------------------------------------------------------------------------
 * HOW TO RUN THIS IN 30 SECONDS:
 * 1. Open your browser and go to: https://script.google.com
 * 2. Click "+ New project".
 * 3. Replace all default code with the contents of this file.
 * 4. Click the "Save" (disk icon) and then "Run" (play button).
 * 5. If prompted, click "Review permissions", choose your Google account, 
 *    click "Advanced" -> "Go to Untitled project (unsafe)" -> "Allow".
 * 6. The "Execution log" at the bottom will print:
 *      - Form Published URL (share with students!)
 *      - Form Edit URL
 *      - Linked Google Sheet URL (where responses are collected)
 * 7. Copy the Form Published URL and paste it into:
 *      resources/config.properties -> form.url=<your-url>
 * --------------------------------------------------------------------------
 */

function createRoomSyncForm() {
  // Create Form
  var form = FormApp.create('RoomSync — Student Preferences & Lifestyle Survey');
  form.setDescription(
    'Welcome to RoomSync!\n\n' +
    'Please complete this survey to discover your academically eligible hostel options ' +
    'and find your top 3 compatible roommate recommendations.\n\n' +
    'Note: RoomSync is recommendation-only. Final room/hostel allocations are handled ' +
    'by the university hostel administration.'
  );
  form.setAllowResponseEdits(true);
  form.setProgressBar(true);

  // =========================================================================
  // SECTION 1: Personal & Academic Details
  // =========================================================================
  // Header is page 1
  form.setTitle('RoomSync — Student Preferences & Lifestyle Survey');
  
  form.addTextItem()
      .setTitle('Full Name')
      .setHelpText('Enter your full legal name.')
      .setRequired(true);

  form.addTextItem()
      .setTitle('Roll Number')
      .setHelpText('University roll number (e.g., 24BCE1001, 23BCE1101).')
      .setRequired(true);

  form.addTextItem()
      .setTitle('University Email')
      .setHelpText('Your official student email address.')
      .setRequired(true);

  form.addTextItem()
      .setTitle('Program')
      .setHelpText('Degree and department, e.g. B.Tech Computer Science, B.Tech Electronics.')
      .setRequired(true);

  form.addMultipleChoiceItem()
      .setTitle('Gender')
      .setHelpText('Roommate matches are only made between students of the same gender.')
      .setChoiceValues(['Male', 'Female'])
      .setRequired(true);

  form.addMultipleChoiceItem()
      .setTitle('Academic Year')
      .setChoiceValues(['1st Year', '2nd Year', '3rd Year', '4th Year'])
      .setRequired(true);

  form.addTextItem()
      .setTitle('Score')
      .setHelpText('1st Year: Enter your admission Percentile (0 to 100). Years 2 to 4: Enter your CGPA (0 to 10).')
      .setRequired(true);

  form.addMultipleChoiceItem()
      .setTitle('Hostel Application Type')
      .setChoiceValues(['First-time', 'Renewal'])
      .setRequired(false);

  // =========================================================================
  // SECTION 2: Hostel Preferences
  // =========================================================================
  form.addPageBreakItem()
      .setTitle('Section 2 — Hostel Preferences')
      .setHelpText('Select your preferred hostels and room occupancy. (RoomSync filters options based on academic cutoffs).');

  form.addCheckboxItem()
      .setTitle('Hostel Preferences')
      .setHelpText('Select the hostels you wish to consider.')
      .setChoiceValues(['Aravali House', 'Nilgiri Residency', 'Shivalik Hall'])
      .setRequired(false);

  form.addCheckboxItem()
      .setTitle('Occupancy Preference')
      .setHelpText('Select all that apply.')
      .setChoiceValues(['Single', 'Double', 'Triple'])
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Preferred Floor / Building')
      .setChoiceValues(['Ground Floor', 'Upper Floors', 'No preference'])
      .setRequired(false);

  form.addParagraphTextItem()
      .setTitle('Specific Accommodation Requirement')
      .setHelpText('Optional: Accessibility, medical, or ground-floor needs.')
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Willing to consider other academically eligible hostel options?')
      .setChoiceValues(['Yes', 'No'])
      .setRequired(false);

  // =========================================================================
  // SECTION 3: Lifestyle & Roommate Preferences
  // =========================================================================
  form.addPageBreakItem()
      .setTitle('Section 3 — Lifestyle & Roommate Preferences')
      .setHelpText('Daily living habits compared by the RoomSync matching algorithm.');

  form.addTextItem()
      .setTitle('Sleep Time')
      .setHelpText('Typical bedtime in 24-hour HH:mm format (e.g. 23:00, 00:30).')
      .setRequired(false);

  form.addTextItem()
      .setTitle('Wake Time')
      .setHelpText('Typical wake-up time in 24-hour HH:mm format (e.g. 07:00, 08:15).')
      .setRequired(false);

  form.addScaleItem()
      .setTitle('Cleanliness')
      .setBounds(1, 10)
      .setLabels('Casual / Relaxed', 'Extremely Clean / Tidy')
      .setRequired(false);

  form.addScaleItem()
      .setTitle('Noise Tolerance')
      .setBounds(1, 10)
      .setLabels('Need Complete Silence', 'High Noise Tolerance')
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Study In Room')
      .setChoiceValues(['Yes', 'Sometimes', 'No'])
      .setRequired(false);

  form.addCheckboxItem()
      .setTitle('Study Time')
      .setHelpText('Select all that apply.')
      .setChoiceValues(['Morning', 'Afternoon', 'Evening', 'Night'])
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Room Environment')
      .setChoiceValues(['Quiet', 'Balanced', 'Lively'])
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Guest Frequency')
      .setChoiceValues(['Never', 'Rarely', 'Sometimes', 'Often'])
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Food Preference')
      .setChoiceValues(['Veg', 'Egg', 'Non-veg'])
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Food Sharing')
      .setChoiceValues(['Yes', 'Sometimes', 'No'])
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Room Organization')
      .setChoiceValues(['Very organized', 'Moderate', 'Relaxed'])
      .setRequired(false);

  form.addTextItem()
      .setTitle('AC Temperature Preference')
      .setHelpText('Optional (e.g., 24C or No preference).')
      .setRequired(false);

  // =========================================================================
  // SECTION 4: Important Requirements & Factors
  // =========================================================================
  form.addPageBreakItem()
      .setTitle('Section 4 — Important Requirements & Priorities')
      .setHelpText('Indicate how important each factor is to you. "Must have" habits prevent incompatible roommate matches.');

  // Grid item: row labels match CompatibilityCalculator factors
  form.addGridItem()
      .setTitle('Importance')
      .setRows([
        'Sleep schedule',
        'Cleanliness',
        'Noise tolerance',
        'Study habits',
        'Daily schedule',
        'Room environment',
        'Guests and social',
        'Food'
      ])
      .setColumns(['Must have', 'Preferable', 'Not important']);

  form.addCheckboxItem()
      .setTitle('Habits You Cannot Tolerate')
      .setChoiceValues([
        'Late night noise / gaming',
        'Smoking / vaping',
        'Messy / unwashed dishes',
        'Frequent unannounced visitors',
        'Non-vegetarian food stored in room'
      ])
      .showOtherOption(true)
      .setRequired(false);

  form.addMultipleChoiceItem()
      .setTitle('Guest Policy Preference')
      .setChoiceValues(['Comfortable', 'Only with restrictions', 'Not comfortable'])
      .setRequired(false);

  form.addParagraphTextItem()
      .setTitle('Other Important Living Preferences')
      .setHelpText('Any specific preferences or personal habits.')
      .setRequired(false);

  // =========================================================================
  // SECTION 5: Consent & Submit
  // =========================================================================
  form.addPageBreakItem()
      .setTitle('Section 5 — Consent & Submission')
      .setHelpText('Confirm your submission to complete your registration.');

  form.addMultipleChoiceItem()
      .setTitle('Consent')
      .setHelpText('I confirm the information provided is accurate and consent to my lifestyle preferences being used for roommate recommendations. I understand recommendations are suggestions only.')
      .setChoiceValues(['Yes I agree', 'No'])
      .setRequired(true);

  // Automatically create a linked Google Sheet for response exports
  var ss = SpreadsheetApp.create('RoomSync Student Responses (Linked Sheet)');
  form.setDestination(FormApp.DestinationType.SPREADSHEET, ss.getId());

  var publishedUrl = form.getPublishedUrl();
  var editUrl = form.getEditUrl();
  var sheetUrl = ss.getUrl();

  Logger.log('=================================================================');
  Logger.log('🎉 GOOGLE FORM & RESPONSE SHEET CREATED SUCCESSFULLY!');
  Logger.log('');
  Logger.log('1. Public Form URL (for students): ' + publishedUrl);
  Logger.log('2. Form Edit URL (for you):         ' + editUrl);
  Logger.log('3. Linked Google Sheet URL:         ' + sheetUrl);
  Logger.log('');
  Logger.log('To connect to RoomSync:');
  Logger.log('Open resources/config.properties and set:');
  Logger.log('form.url=' + publishedUrl);
  Logger.log('=================================================================');
}

