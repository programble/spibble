require 'fileutils'
require 'lastfm'
require 'taglib'

class Spibble
  API_KEY = '7de0384350e7b5eb96c942861527f335'
  API_SECRET = '936dadfe60ad6e86101dda1e7fd1c7b8'

  SPIBBLE_DIR = File.join(Dir.home, '.config', 'spibble')
  SESSION_FILE = File.join(SPIBBLE_DIR, 'session')
  SPLIT_FILE = File.join(SPIBBLE_DIR, 'splits')

  Track = Struct.new(:title, :artist, :track, :length)

  def initialize(directory)
    @lastfm = Lastfm.new(API_KEY, API_SECRET)
    get_session

    load_splits

    @tracks = []
    read_tracks(directory)
    get_splits unless @splits.include? @album
  end

  def get_session
    if File.exist? SESSION_FILE
      @lastfm.session = File.read(SESSION_FILE)
    else
      token = @lastfm.auth.get_token

      puts "http://www.last.fm/api/auth/?api_key=#{API_KEY}&token=#{token}"
      print "Once you have authorized Spibble, press enter... "
      $stdin.gets

      begin
        @lastfm.session = @lastfm.auth.get_session(token: token)['key']
      rescue Lastfm::ApiError => e
        puts "Error: #{e}"
        exit 1
      end

      FileUtils.mkdir_p(SPIBBLE_DIR)
      File.write(SESSION_FILE, @lastfm.session)
    end
  end

  def load_splits
    if File.exist? SPLIT_FILE
      File.open(SPLIT_FILE) do |f|
        @splits = Marshal.load(f)
      end
    else
      @splits = {}
    end
  end

  def save_splits
    FileUtils.mkdir_p(SPIBBLE_DIR)
    File.open(SPLIT_FILE, 'w') do |f|
      Marshal.dump(@splits, f)
    end
  end

  def read_tracks(directory)
    Dir.foreach(directory) do |file|
      file = File.join(directory, file)
      next unless File.file? file

      track = TagLib::FileRef.open(file) do |f|
        if f.null?
          nil
        else
          tag, prop = f.tag, f.audio_properties
          @album ||= tag.album
          if tag.album != @album
            puts 'Error: not all files are from same album'
            exit 1
          end
          Track.new(tag.title, tag.artist, tag.track, prop.length)
        end
      end
      @tracks << track if track
    end

    @tracks.sort_by! {|t| t.track }
  end

  def get_splits
    print "Splits for #{@album}: "
    splits = $stdin.gets.chomp.split(/[\s,]+/).map(&:to_i)
    # TODO: Validate input (order)
    splits.delete(0) # Ignore invalid input
    splits.unshift(1)

    @splits[@album] = splits
    save_splits
  end

  def scrobble
    side = 'A'
    splits = @splits[@album].dup
    @tracks.each do |track|
      if track.track == splits.first
        print "Scrobble side #{side}? "
        $stdin.gets
        splits.shift
        side.succ!
      end

      puts format('%02d - %s', track.track, track.title)
      scrobble = {artist: track.artist, track: track.title, album: @album,
                  trackNumber: track.track, duration: track.length,
                  timestamp: Time.now.utc.to_i}
      @lastfm.track.update_now_playing(scrobble.dup)
      sleep(track.length)
      @lastfm.track.scrobble(scrobble)
    end
  end
end
