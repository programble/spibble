require 'lastfm'

module Spibble
  class Lastfm
    ApiError = ::Lastfm::ApiError

    API_KEY = '7de0384350e7b5eb96c942861527f335'
    API_SECRET = '936dadfe60ad6e86101dda1e7fd1c7b8'

    def initialize(session)
      @lastfm = ::Lastfm.new(API_KEY, API_SECRET)

      if File.exist? session
        @lastfm.session = File.read(session)
      else
        token = @lastfm.auth.get_token

        puts "http://www.last.fm/api/auth/?api_key=#{API_KEY}&token=#{token}"
        print 'Once you have authorized Spibble, press enter... '
        $stdin.gets

        @lastfm.session = @lastfm.auth.get_session(token: token)['key']

        File.write(session, @lastfm.session)
      end
    end

    def now_playing(album, track)
      @scrobble = {artist: album.artist, album: album.title,
        track: track.title, trackNumber: track.number, duration: track.length,
        timestamp: Time.now.utc.to_i}
      @lastfm.track.update_now_playing(@scrobble.dup)
    end

    def scrobble
      @lastfm.track.scrobble(@scrobble.dup)
    end
  end
end
