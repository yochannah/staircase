/* global browser sleep element describe beforeEach it expect */

(function () {
  'use strict';

  describe('Steps', function () {
    beforeEach(function () {
      browser.navigateTo('/');
      sleep(1);
    });

    describe('view 1', function () {
      beforeEach(function () {
        browser.navigateTo('/view1');
        sleep(1);
      });

      it('should...', function () {
      });
    });
  });
})();
