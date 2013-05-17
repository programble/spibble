require 'fileutils'

require 'spibble/lastfm'

module Spibble
  SPIBBLE_DIR = File.join(Dir.home, '.config', 'spibble')

  class Application
    def initialize
      FileUtils.mkdir_p(SPIBBLE_DIR)

      @database = Database.new(File.join(SPIBBLE_DIR, 'albums.yml'))

      begin
        @lastfm = Lastfm.new(File.join(SPIBBLE_DIR, 'session'))
      rescue Lastfm::ApiError => e
        puts "Error: #{e}"
        exit 1
      end
    end
  end
end
